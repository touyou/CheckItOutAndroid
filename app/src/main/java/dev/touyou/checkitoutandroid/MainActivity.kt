package dev.touyou.checkitoutandroid

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import dev.touyou.checkitoutandroid.databinding.MainActivityBinding
import dev.touyou.checkitoutandroid.entity.PlayMode
import dev.touyou.checkitoutandroid.entity.SoundData
import dev.touyou.checkitoutandroid.ui.main.ControlFragment
import dev.touyou.checkitoutandroid.ui.main.PadFragment
import dev.touyou.checkitoutandroid.ui.main.PadViewModel
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import kotlinx.coroutines.launch
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_AUDIO_PERMISSION = 1
    }

    private var privateMode = 0
    private val prefName = "initial-run"

    lateinit var viewModel: PadViewModel
    private lateinit var binding: MainActivityBinding
    private var foldingFeature: FoldingFeature? = null
    private var appliedArrangement: Arrangement? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        ensurePermissionAllowed()

        initRealm()

        viewModel = ViewModelProvider(this)[PadViewModel::class.java]

        initSounds()
        println(viewModel.sounds)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.padContainer, PadFragment.newInstance())
                .commitNow()
            supportFragmentManager.beginTransaction()
                .replace(R.id.controlContainer, ControlFragment.newInstance())
                .commitNow()
        }

        initButton()
        observeWindowLayout()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty()) return
        when (requestCode) {
            REQUEST_CODE_AUDIO_PERMISSION -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "録音にマイクを使用します。",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    // IMO: move to application extended class ?
    private fun initRealm() {
        // Realm Kotlinでは特に初期化処理は不要
    }

    private fun initButton() {
        binding.playButton.setOnClickListener { viewModel.changeMode(PlayMode.PLAY) }
        binding.recButton.setOnClickListener { viewModel.changeMode(PlayMode.REC) }
        binding.editButton.setOnClickListener { viewModel.changeMode(PlayMode.EDIT) }
        binding.licensesButton.setOnClickListener { showLicenses() }
        // 選択中のモードのボタンだけ薄くする。表示はモードから決め、作り直しても揃うようにする
        viewModel.currentMode.observe(this) { mode ->
            binding.playButton.alpha = if (mode == PlayMode.PLAY) 0.3f else 1.0f
            binding.recButton.alpha = if (mode == PlayMode.REC) 0.3f else 1.0f
            binding.editButton.alpha = if (mode == PlayMode.EDIT) 0.3f else 1.0f
        }
    }

    // 同梱ライブラリとフォントのライセンス文。依存を足したら res/raw/licenses.txt も更新する
    private fun showLicenses() {
        val text = resources.openRawResource(R.raw.licenses).bufferedReader().use { it.readText() }
        val padding = (16 * resources.displayMetrics.density).toInt()
        val body = TextView(this).apply {
            this.text = text
            textSize = 12f
            setPadding(padding, padding, padding, padding)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.licenses)
            .setView(ScrollView(this).apply { addView(body) })
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun initSounds() {
        val prefs = getSharedPreferences(prefName, privateMode)
        if (!prefs.getBoolean(prefName, false)) {
            viewModel.addSound("バスドラ", 0, rawId = R.raw.touyou, id = 0)
            viewModel.addSound("みんなで", 1, rawId = R.raw.minnade_cut, id = 1)
            viewModel.addSound("チェケラ", 2, rawId = R.raw.chekera_cut, id = 2)

            prefs.edit {
                this.putBoolean(prefName, true)
                this.commit()
            }
        }
    }

    // edge-to-edge なので、背景画像だけ全面に敷き、パッドと操作列はシステムバーとカットアウトを避ける
    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.content) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }
    }

    private fun observeWindowLayout() {
        binding.content.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ -> view.post { applyArrangement() } }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { info ->
                        foldingFeature = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                        applyArrangement()
                    }
            }
        }
    }

    /**
     * iOS 版と同じく、ウィンドウの形で並べ方を決める。横長は左にパッド・右に操作列、縦長は上にパッド・
     * 下に操作列。半開きの折りたたみでは折り目に沿って、16 個のパッドを片側にまとめる。
     * 向きやサイズの変化では Activity を作り直さないので、ここで並べ直す。
     */
    private fun applyArrangement() {
        val content = binding.content
        val width = content.width - content.paddingLeft - content.paddingRight
        val height = content.height - content.paddingTop - content.paddingBottom
        if (width <= 0 || height <= 0) return
        val arrangement = arrangementFor(width, height, content)
        if (arrangement == appliedArrangement) return
        appliedArrangement = arrangement

        val pad = R.id.padContainer
        val column = R.id.controlColumn
        val parent = ConstraintSet.PARENT_ID
        ConstraintSet().apply {
            clone(content)
            clear(pad)
            clear(column)
            constrainWidth(pad, arrangement.padWidth)
            constrainHeight(pad, arrangement.padHeight)
            constrainWidth(column, ConstraintSet.MATCH_CONSTRAINT)
            constrainHeight(column, ConstraintSet.MATCH_CONSTRAINT)
            if (arrangement.sideBySide) {
                // 折り目の位置は左からの距離なので、RTL でも左右は物理的な向きでつなぐ
                connect(pad, ConstraintSet.LEFT, parent, ConstraintSet.LEFT)
                connect(pad, ConstraintSet.TOP, parent, ConstraintSet.TOP)
                connect(pad, ConstraintSet.BOTTOM, parent, ConstraintSet.BOTTOM)
                connect(column, ConstraintSet.LEFT, pad, ConstraintSet.RIGHT, arrangement.gap)
                connect(column, ConstraintSet.RIGHT, parent, ConstraintSet.RIGHT)
                connect(column, ConstraintSet.TOP, parent, ConstraintSet.TOP)
                connect(column, ConstraintSet.BOTTOM, parent, ConstraintSet.BOTTOM)
            } else {
                connect(pad, ConstraintSet.TOP, parent, ConstraintSet.TOP)
                connect(pad, ConstraintSet.START, parent, ConstraintSet.START)
                connect(pad, ConstraintSet.END, parent, ConstraintSet.END)
                connect(column, ConstraintSet.TOP, pad, ConstraintSet.BOTTOM, arrangement.gap)
                connect(column, ConstraintSet.BOTTOM, parent, ConstraintSet.BOTTOM)
                connect(column, ConstraintSet.START, parent, ConstraintSet.START)
                connect(column, ConstraintSet.END, parent, ConstraintSet.END)
            }
            applyTo(content)
        }
    }

    private fun arrangementFor(width: Int, height: Int, content: ConstraintLayout): Arrangement {
        val fold = foldingFeature?.takeIf { it.isSeparating }
        val sideBySide = fold?.let { it.orientation == FoldingFeature.Orientation.VERTICAL } ?: (width >= height)
        val total = if (sideBySide) width else height
        val minPane = (160 * resources.displayMetrics.density).toInt()
        // 折り目は window 座標で来るので、パッドと操作列を置く領域の位置を引いてから使う
        val split = fold?.let {
            val origin = IntArray(2).also(content::getLocationInWindow)
            val start = if (sideBySide) {
                it.bounds.left - origin[0] - content.paddingLeft
            } else {
                it.bounds.top - origin[1] - content.paddingTop
            }
            val gap = if (sideBySide) it.bounds.width() else it.bounds.height()
            (start to gap).takeIf { start >= minPane && start + gap <= total - minPane }
        }
        return when {
            split != null && sideBySide -> Arrangement(true, split.first, height, split.second)
            split != null -> Arrangement(false, min(width, split.first), split.first, split.second)
            sideBySide -> Arrangement(true, width / 2, height, 0)
            // 縦長: パッドは正方形にして、操作列(サウンド一覧と録音・再生・編集)の高さを残す
            else -> min(width, height * 3 / 5).let { side -> Arrangement(false, side, side, 0) }
        }
    }

    private data class Arrangement(val sideBySide: Boolean, val padWidth: Int, val padHeight: Int, val gap: Int)

    private fun ensurePermissionAllowed() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                REQUEST_CODE_AUDIO_PERMISSION
            )
        }
    }
}
