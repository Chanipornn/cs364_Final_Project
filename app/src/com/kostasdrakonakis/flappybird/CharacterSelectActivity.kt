package com.kostasdrakonakis.flappybird

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class CharacterSelectActivity : Activity() {

    private var index = 0
    private lateinit var img: ImageView
    private lateinit var txtName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_character_select)

        img = findViewById(R.id.imgPreview)
        txtName = findViewById(R.id.txtName)
        index = savedInstanceState?.getInt(STATE_INDEX) ?: GamePrefs.getCharacter(this)
        show()

        val size = Characters.list.size
        findViewById<Button>(R.id.btnPrev).setOnClickListener {
            index = (index - 1 + size) % size
            show()
        }
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            index = (index + 1) % size
            show()
        }
        findViewById<Button>(R.id.btnSelect).setOnClickListener {
            GamePrefs.setCharacter(this, index)
            finish()
        }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_INDEX, index)
    }

    private fun show() {
        val skin = Characters.list[index]
        txtName.setText(skin.nameRes)
        // โหลดจาก assets ชุดเดียวกับที่เกมใช้
        assets.open(skin.frameA).use { img.setImageBitmap(BitmapFactory.decodeStream(it)) }
    }

    companion object {
        private const val STATE_INDEX = "state_index"
    }
}
