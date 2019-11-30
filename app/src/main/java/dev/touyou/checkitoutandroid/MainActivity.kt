package dev.touyou.checkitoutandroid

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProviders
import dev.touyou.checkitoutandroid.entity.PlayMode
import dev.touyou.checkitoutandroid.entity.soundDao
import dev.touyou.checkitoutandroid.ui.main.ControlFragment
import dev.touyou.checkitoutandroid.ui.main.PadFragment
import dev.touyou.checkitoutandroid.ui.main.PadViewModel
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_AUDIO_PERMISSION = 1
        const val REQUEST_CODE_STORAGE_PERMISSION = 2
    }

    private var privateMode = 0
    private val prefName = "initial-run"

    lateinit var viewModel: PadViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.main_activity)

        ensurePermissionAllowed()

        initRealm()

        viewModel = ViewModelProviders.of(this).get(PadViewModel::class.java)

        initSounds()

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
        Realm.init(this)
        val config = RealmConfiguration.Builder()
            .name("sounddb.realm")
            .schemaVersion(1)
            .build()
        Realm.setDefaultConfiguration(config)
    }

    private fun initButton() {
        playButton.alpha = 0.3f
        playButton.setOnClickListener {
            viewModel.changeMode(PlayMode.PLAY)
            playButton.alpha = 0.3f
            recButton.alpha = 1.0f
            editButton.alpha = 1.0f
        }
        recButton.setOnClickListener {
            viewModel.changeMode(PlayMode.REC)
            playButton.alpha = 1.0f
            recButton.alpha = 0.3f
            editButton.alpha = 1.0f
        }
        editButton.setOnClickListener {
            viewModel.changeMode(PlayMode.EDIT)
            playButton.alpha = 1.0f
            recButton.alpha = 1.0f
            editButton.alpha = 0.3f
        }
    }

    private fun initSounds() {
        val prefs = getSharedPreferences(prefName, privateMode)
        if (!prefs.getBoolean(prefName, false)) {
            val realm = Realm.getDefaultInstance()
            realm.soundDao().addToSound("バスドラ", 0, rawId = R.raw.touyou)
            realm.soundDao().addToSound("みんなで", 1, rawId = R.raw.minnade_cut)
            realm.soundDao().addToSound("チェケラ", 2, rawId = R.raw.chekera_cut)

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
    }
}
