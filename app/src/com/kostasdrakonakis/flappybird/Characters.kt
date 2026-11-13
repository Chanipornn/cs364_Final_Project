package com.kostasdrakonakis.flappybird

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.StringRes

data class CharacterSkin(
    @StringRes val nameRes: Int,
    val frameA: String,
    val frameB: String
)

object Characters {
    // เพิ่มตัวละครใหม่: ใส่รูป 2 เฟรมขนาดเท่ากันใน assets แล้วเพิ่มบรรทัดที่นี่
    // (อย่าเปลี่ยนลำดับตัวเดิม เพราะ index ถูกบันทึกไว้ใน SharedPreferences)
    val list = listOf(
        CharacterSkin(R.string.char_classic, "bird.png", "bird2.png"),
        CharacterSkin(R.string.char_red, "char1_a.png", "char1_b.png"),
        CharacterSkin(R.string.char_blue, "char2_a.png", "char2_b.png"),
        CharacterSkin(R.string.char_bear, "bear_a.png", "bear_b.png"),
        CharacterSkin(R.string.char_cat, "cat_a.png", "cat_b.png"),
        CharacterSkin(R.string.char_frog, "frog_a.png", "frog_b.png"),
        CharacterSkin(R.string.char_pig, "pig_a.png", "pig_b.png")
    )

    /** โหลดรูปจาก assets เป็น drawable แบบไม่เบลอ (พิกเซลอาร์ตจะคมเมื่อขยาย) */
    fun loadDrawable(ctx: Context, fileName: String): BitmapDrawable {
        val bmp = ctx.assets.open(fileName).use { BitmapFactory.decodeStream(it) }
        return BitmapDrawable(ctx.resources, bmp).apply { setFilterBitmap(false) }
    }
}
