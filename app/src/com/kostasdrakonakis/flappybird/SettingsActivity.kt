package com.kostasdrakonakis.flappybird

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button

class SettingsActivity : Activity() {

    // ใส่ภาษา/ธีมที่เลือกให้หน้านี้ (จากขั้นที่ 1)
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SettingsHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnThai = findViewById<Button>(R.id.btnThai)
        val btnEnglish = findViewById<Button>(R.id.btnEnglish)
        val btnDay = findViewById<Button>(R.id.btnDay)
        val btnNight = findViewById<Button>(R.id.btnNight)

        // ภาษาที่ใช้อยู่จริงตอนนี้ (ถ้ายังไม่เคยเลือกจะเป็นภาษาเครื่อง)
        val isThai = resources.configuration.locales[0].language == "th"
        val isNight = GamePrefs.isNightMode(this)
        highlight(btnThai, isThai)
        highlight(btnEnglish, !isThai)
        highlight(btnDay, !isNight)
        highlight(btnNight, isNight)

        btnThai.setOnClickListener { changeLanguage("th") }
        btnEnglish.setOnClickListener { changeLanguage("en") }
        btnDay.setOnClickListener { changeNightMode(false) }
        btnNight.setOnClickListener { changeNightMode(true) }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    /** ปุ่มที่เลือกอยู่ = สีเขียว, ที่ไม่ได้เลือก = สีฟ้า */
    private fun highlight(btn: Button, selected: Boolean) {
        btn.setBackgroundResource(if (selected) R.drawable.btn_game_green else R.drawable.btn_game_blue)
    }

    private fun changeLanguage(lang: String) {
        GamePrefs.setLanguage(this, lang)
        recreate()   // โหลดหน้าใหม่ให้ข้อความเปลี่ยนภาษาทันที
    }

    private fun changeNightMode(night: Boolean) {
        GamePrefs.setNightMode(this, night)
        recreate()
    }
}