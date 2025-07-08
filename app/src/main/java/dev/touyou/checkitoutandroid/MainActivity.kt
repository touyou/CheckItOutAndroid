package dev.touyou.checkitoutandroid

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
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
        const val REQUEST_CODE_STORAGE_PERMISSION = 2
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
            REQUEST_CODE_STORAGE_PERMISSION -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "録音ファイルの保存にストレージを使用します。",
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
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_STORAGE_PERMISSION
            )
        }
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_STORAGE_PERMISSION
            )
        }
    }
}
