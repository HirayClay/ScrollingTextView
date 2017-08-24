package io.hirayclay.scrollingtextview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scrolling_textview.bindText(arrayListOf("AAA", "BBB", "CCC", "DDD", "EEE", "FFF", "GGG", "HHH", "JJJ", "KKK", "LLL", "MMM", "OOO"))
        quick_reset.setOnClickListener { scrolling_textview.reset(false) }
        smooth_reset.setOnClickListener { scrolling_textview.reset(true) }
        fake_scroll.setOnClickListener { scrolling_textview.fakeScroll(10,300) }
    }
}
