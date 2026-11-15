package com.kostasdrakonakis.flappybird

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import java.io.ByteArrayOutputStream
import kotlin.math.ceil

/**
 * วาดข้อความในเกมด้วยระบบตัวอักษรของ Android แทน BitmapFont ของ libGDX
 * เพราะ BitmapFont ไม่มีตัวอักษรไทย ส่วน Android วางสระ/วรรณยุกต์ไทยได้ถูกต้อง
 * วิธีทำ: วาดข้อความลง Bitmap -> แปลงเป็น Texture -> เก็บไว้ใช้ซ้ำ (cache)
 */
class GameText {

    // ใช้ฟอนต์ตัวหนาของระบบ ซึ่งมีทั้งตัวอักษรอังกฤษ ตัวเลข และภาษาไทย
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
    }
    private val bounds = Rect()

    private class Entry(val texture: Texture, val baseline: Float)   // baseline = ระยะจากขอบล่างภาพถึงเส้นบรรทัด
    private val cache = HashMap<String, Entry>()

    /**
     * วาดข้อความกึ่งกลางที่ centerX โดย topY คือขอบบนของตัวอักษร (ตำแหน่งเดียวกับ BitmapFont เดิม)
     * sizePx = ขนาดตัวอักษรเป็นพิกเซล, color = สีที่ต้องการ (ข้อความวาดเป็นสีขาวแล้วย้อมสีตอนวาด)
     */
    fun draw(batch: SpriteBatch, text: String, centerX: Float, topY: Float, sizePx: Float, color: Color) {
        if (text.isEmpty()) return
        val key = "${sizePx.toInt()}|$text"
        var e = cache[key]
        if (e == null) {
            // ข้อความที่เปลี่ยนบ่อย ทำให้ cache โตเรื่อย ๆ จึงล้างทิ้งเมื่อเกิน 80 รายการ
            // ต้อง flush ก่อน เพื่อให้ข้อความที่สั่งวาดไปแล้วในเฟรมนี้ถูกวาดก่อนลบ Texture
            if (cache.size > 80) {
                batch.flush()
                cache.values.forEach { it.texture.dispose() }
                cache.clear()
            }
            e = render(text, sizePx.toInt().toFloat())
            cache[key] = e
        }
        val tex = e.texture
        val capHeight = sizePx * 0.72f                       // ความสูงตัวพิมพ์ใหญ่โดยประมาณ
        val x = centerX - tex.width / 2f
        val y = topY - capHeight - e.baseline
        val old = batch.color.cpy()
        batch.color = color
        batch.draw(tex, x, y)
        batch.color = old
    }

    /** วาดข้อความ 1 ชุดลง Bitmap: ตัวขาว + ขอบดำ แล้วแปลงเป็น Texture ของ libGDX */
    private fun render(text: String, sizePx: Float): Entry {
        paint.textSize = sizePx
        paint.getTextBounds(text, 0, text.length, bounds)
        val stroke = sizePx * 0.10f
        val pad = ceil(stroke + sizePx * 0.15f).toInt()
        val w = maxOf(bounds.width() + pad * 2, 1)
        val h = maxOf(bounds.height() + pad * 2, 1)

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val x = pad - bounds.left.toFloat()
        val baseY = pad - bounds.top.toFloat()

        paint.style = Paint.Style.STROKE                 // ขอบดำ ช่วยให้อ่านง่ายบนพื้นหลังสว่าง
        paint.strokeWidth = stroke
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = android.graphics.Color.argb(200, 0, 0, 0)
        canvas.drawText(text, x, baseY, paint)
        paint.style = Paint.Style.FILL                   // ตัวอักษรสีขาว
        paint.color = android.graphics.Color.WHITE
        canvas.drawText(text, x, baseY, paint)

        // Bitmap ของ Android -> PNG -> Pixmap -> Texture ของ libGDX
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        bmp.recycle()
        val bytes = out.toByteArray()
        val pixmap = Pixmap(bytes, 0, bytes.size)
        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()

        return Entry(texture, (bounds.bottom + pad).toFloat())
    }

    fun dispose() {
        cache.values.forEach { it.texture.dispose() }
        cache.clear()
    }

}
/**
 * แปลงรูป drawable ของ Android (เช่นปุ่มเมนู btn_game_green.xml) เป็น Texture ของ libGDX
 * เพื่อให้ปุ่มในเกมหน้าตาเหมือนปุ่มในหน้าเมนู (มุมมน + ไล่สี + ขอบ + เงา)
 * w, h = ขนาดปุ่มเป็นพิกเซล (สร้างให้พอดีขนาดจริง ภาพจะได้คมไม่เบลอ)
 */
fun drawableTexture(context: Context, resId: Int, w: Int, h: Int): Texture {
    val drawable = ContextCompat.getDrawable(context, resId)!!
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(Canvas(bmp))

    // Bitmap ของ Android -> PNG -> Pixmap -> Texture ของ libGDX (วิธีเดียวกับข้อความ)
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    bmp.recycle()
    val bytes = out.toByteArray()
    val pixmap = Pixmap(bytes, 0, bytes.size)
    val texture = Texture(pixmap)
    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    pixmap.dispose()
    return texture
}