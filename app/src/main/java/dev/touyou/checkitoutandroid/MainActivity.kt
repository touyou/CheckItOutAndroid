package dev.touyou.checkitoutandroid

import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import dev.touyou.checkitoutandroid.databinding.MainActivityBinding
import dev.touyou.checkitoutandroid.entity.PlayMode
import dev.touyou.checkitoutandroid.entity.SoundData
import dev.touyou.checkitoutandroid.ui.main.ControlFragment
import dev.touyou.checkitoutandroid.ui.main.PadFragment
import dev.touyou.checkitoutandroid.ui.main.PadViewModel
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_AUDIO_PERMISSION = 1
    }

    private var privateMode = 0
    private val prefName = "initial-run"

    lateinit var viewModel: PadViewModel
    private lateinit var binding: MainActivityBinding

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
        binding.playButton.alpha = 0.3f
        binding.playButton.setOnClickListener {
            viewModel.changeMode(PlayMode.PLAY)
            binding.playButton.alpha = 0.3f
            binding.recButton.alpha = 1.0f
            binding.editButton.alpha = 1.0f
        }
        binding.recButton.setOnClickListener {
            viewModel.changeMode(PlayMode.REC)
            binding.playButton.alpha = 1.0f
            binding.recButton.alpha = 0.3f
            binding.editButton.alpha = 1.0f
        }
        binding.editButton.setOnClickListener {
            viewModel.changeMode(PlayMode.EDIT)
            binding.playButton.alpha = 1.0f
            binding.recButton.alpha = 1.0f
            binding.editButton.alpha = 0.3f
        }
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

    // targetSdk 35 以降は edge-to-edge が強制されるため、背景画像だけ全面に敷いて操作部はシステムバーとカットアウトを避ける
    private fun applySystemBarInsets() {
        val pad = binding.padContainer
        val column = binding.controlColumn
        val padBase = Rect(pad.paddingLeft, pad.paddingTop, pad.paddingRight, pad.paddingBottom)
        val columnBase =
            Rect(column.paddingLeft, column.paddingTop, column.paddingRight, column.paddingBottom)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            pad.setPadding(
                padBase.left + insets.left,
                padBase.top + insets.top,
                padBase.right,
                padBase.bottom + insets.bottom
            )
            column.setPadding(
                columnBase.left,
                columnBase.top + insets.top,
                columnBase.right + insets.right,
                columnBase.bottom + insets.bottom
            )
            windowInsets
        }
    }

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
