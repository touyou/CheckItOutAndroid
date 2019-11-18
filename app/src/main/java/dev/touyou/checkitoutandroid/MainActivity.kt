package dev.touyou.checkitoutandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import dev.touyou.checkitoutandroid.ui.main.ControlFragment
import dev.touyou.checkitoutandroid.ui.main.PadFragment
import dev.touyou.checkitoutandroid.ui.main.PadViewModel
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {

    lateinit var viewModel: PadViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        viewModel = ViewModelProviders.of(this).get(PadViewModel::class.java)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.padContainer, PadFragment.newInstance())
                .commitNow()
            supportFragmentManager.beginTransaction()
                .replace(R.id.controlContainer, ControlFragment.newInstance())
                .commitNow()
        }

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


}
