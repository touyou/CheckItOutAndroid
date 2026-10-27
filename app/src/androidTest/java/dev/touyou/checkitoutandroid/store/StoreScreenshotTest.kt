package dev.touyou.checkitoutandroid.store

import android.content.Context
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import dev.touyou.checkitoutandroid.MainActivity
import dev.touyou.checkitoutandroid.R
import dev.touyou.checkitoutandroid.entity.SoundData
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Play ストアに載せる画面を撮る。動作の検証ではない。
 * .github/workflows/store-screenshots.yml がエミュレータで回し、filesDir/store-shots を取り出す。
 */
@RunWith(AndroidJUnit4::class)
class StoreScreenshotTest {

    @get:Rule
    val permission: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun captureStoreScreenshots() {
        seed()
        ActivityScenario.launch(MainActivity::class.java).use {
            settle()
            capture("01-play")

            onView(withId(R.id.editButton)).perform(click())
            onView(withId(R.id.pad6)).perform(click())
            settle()
            capture("02-edit")

            onView(withId(R.id.recButton)).perform(click())
            onView(withId(R.id.displayNameText))
                .perform(replaceText("チェックイットアウト！"), closeSoftKeyboard())
            settle()
            capture("03-rec")
        }
    }

    // iOS の撮影用データと同じ並び。同梱の 3 音以外はファイルが無く、撮影中は鳴らないだけ
    private fun seed() {
        context.getSharedPreferences("initial-run", Context.MODE_PRIVATE)
            .edit(commit = true) { putBoolean("initial-run", true) }
        val config = RealmConfiguration.Builder(schema = setOf(SoundData::class))
            .name("sounddb.realm")
            .schemaVersion(1)
            .build()
        Realm.deleteRealm(config)
        val raw = mapOf(0 to R.raw.touyou, 1 to R.raw.minnade_cut, 2 to R.raw.chekera_cut)
        val names = listOf(
            "バスドラ", "みんなで", "チェケラ", "イェー！", "拍手", "スネア", "ハイハット", "スクラッチ",
            "歓声", "ドラムロール", "かんぱーい", "ピンポン", "ブブー", "おつかれ", "エアホーン",
            "笑い声", "ジングル",
        )
        Realm.open(config).use { realm ->
            realm.writeBlocking {
                names.forEachIndexed { index, name ->
                    copyToRealm(SoundData().apply {
                        id = index.toLong()
                        displayName = name
                        padNum = if (index < 15) index else -1
                        isRaw = raw.containsKey(index)
                        rawId = raw[index]
                    })
                }
            }
        }
    }

    private fun settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(1500)
    }

    // adb から送ったステータスバーの固定（9:41・電池満タン）を SystemUI が取りこぼすことがあるので、撮る直前にも送る
    private fun pinStatusBar() {
        val demo = "am broadcast -a com.android.systemui.demo -e command"
        listOf(
            "settings put global sysui_demo_allowed 1",
            "$demo enter",
            "$demo clock -e hhmm 0941",
            "$demo battery -e level 100 -e plugged false",
            "$demo network -e wifi show -e level 4 -e fully true",
            "$demo network -e mobile show -e datatype null -e level 4 -e fully true",
            "$demo notifications -e visible false",
        ).forEach { command ->
            ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).use { it.readBytes() }
        }
        // アイコンの描き替えが追いつくまで待つ
        Thread.sleep(1500)
    }

    private fun capture(name: String) {
        pinStatusBar()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            ?: error("スクリーンショットが撮れなかった: $name")
        val dir = File(context.filesDir, "store-shots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

private inline fun <T> Realm.use(block: (Realm) -> T): T = try {
    block(this)
} finally {
    close()
}
