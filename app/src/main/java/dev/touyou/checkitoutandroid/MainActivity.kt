package dev.touyou.checkitoutandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import dev.touyou.checkitoutandroid.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.padContainer, MainFragment.newInstance())
                .commitNow()
        }
    }

}
