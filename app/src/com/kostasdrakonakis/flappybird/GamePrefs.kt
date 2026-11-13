package com.kostasdrakonakis.flappybird

import android.content.Context

/** ที่เก็บค่าตั้งต้นของเกม ใช้ร่วมกันทั้งทีม (คนที่ 2, 3 เพิ่ม key ของตัวเองในไฟล์นี้) */
object GamePrefs {
    private const val NAME = "game_prefs"
    private const val KEY_CHARACTER = "character_index"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getCharacter(ctx: Context): Int {
        val i = prefs(ctx).getInt(KEY_CHARACTER, 0)
        return if (i in Characters.list.indices) i else 0
    }

    fun setCharacter(ctx: Context, index: Int) {
        prefs(ctx).edit().putInt(KEY_CHARACTER, index).apply()
    }
}
