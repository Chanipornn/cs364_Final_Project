package com.kostasdrakonakis.flappybird

import android.content.Context

/** Data class สำหรับเก็บข้อมูล Game State เพื่อใช้บันทึกและกู้คืน */
data class GameStateData(
    val passed: Int,
    val bonus: Int,
    val level: Int,
    val scoringTube: Int,
    val birdY: Float,
    val velocity: Float,
    val hearts: Int,
    val invincible: Float,
    val respawnHover: Float,
    val tubeX: FloatArray,
    val tubeOffset: FloatArray,
    val itemType: IntArray,
    val itemY: FloatArray,
    val itemPhase: FloatArray,
    val tubeCur: Int,
    val tubePrev: Int,
    val tubeFade: Float,
    val screenW: Int = 0,   // ขนาดจอตอนบันทึก ใช้ปรับตำแหน่งเมื่อกู้คืนคนละแนว
    val screenH: Int = 0
)

/** ที่เก็บค่าตั้งต้นของเกม ใช้ร่วมกันทั้งทีม (คนที่ 2, 3 เพิ่ม key ของตัวเองในไฟล์นี้) */
object GamePrefs {
    /** จำนวนท่อในเกม (ต้องตรงกับ FlappyBird) ใช้ตรวจขนาดข้อมูลตอนบันทึก/กู้คืน */
    const val NUM_TUBES = 8

    private const val NAME = "game_prefs"
    private const val KEY_CHARACTER = "character_index"
    private const val KEY_LANGUAGE = "language"      // "th", "en" หรือไม่มีค่า = ตามภาษาเครื่อง
    private const val KEY_NIGHT_MODE = "night_mode"  // true = กลางคืน
    private const val KEY_HIGH_SCORE = "high_score"

    // Key สำหรับ Game State (แยกส่วนจาก High Score)
    private const val KEY_HAS_SAVED_GAME = "has_saved_game"
    private const val KEY_STATE_PASSED = "state_passed"
    private const val KEY_STATE_BONUS = "state_bonus"
    private const val KEY_STATE_LEVEL = "state_level"
    private const val KEY_STATE_SCORING_TUBE = "state_scoring_tube"
    private const val KEY_STATE_BIRD_Y = "state_bird_y"
    private const val KEY_STATE_VELOCITY = "state_velocity"
    private const val KEY_STATE_HEARTS = "state_hearts"
    private const val KEY_STATE_INVINCIBLE = "state_invincible"
    private const val KEY_STATE_RESPAWN_HOVER = "state_respawn_hover"
    private const val KEY_STATE_TUBE_X = "state_tube_x"
    private const val KEY_STATE_TUBE_OFFSET = "state_tube_offset"
    private const val KEY_STATE_ITEM_TYPE = "state_item_type"
    private const val KEY_STATE_ITEM_Y = "state_item_y"
    private const val KEY_STATE_ITEM_PHASE = "state_item_phase"
    private const val KEY_STATE_TUBE_CUR = "state_tube_cur"
    private const val KEY_STATE_TUBE_PREV = "state_tube_prev"
    private const val KEY_STATE_TUBE_FADE = "state_tube_fade"
    private const val KEY_STATE_SCREEN_W = "state_screen_w"
    private const val KEY_STATE_SCREEN_H = "state_screen_h"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getCharacter(ctx: Context): Int {
        val i = prefs(ctx).getInt(KEY_CHARACTER, 0)
        return if (i in Characters.list.indices) i else 0
    }

    fun setCharacter(ctx: Context, index: Int) {
        prefs(ctx).edit().putInt(KEY_CHARACTER, index).apply()
    }
    // ===== ภาษา =====
    /** คืนค่า null ถ้ายังไม่เคยเลือก (ใช้ภาษาตามเครื่อง) */
    fun getLanguage(ctx: Context): String? = prefs(ctx).getString(KEY_LANGUAGE, null)

    fun setLanguage(ctx: Context, lang: String) {
        prefs(ctx).edit().putString(KEY_LANGUAGE, lang).apply()
    }

    // ===== ธีมกลางวัน/กลางคืน =====
    fun isNightMode(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_NIGHT_MODE, false)

    fun setNightMode(ctx: Context, night: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_NIGHT_MODE, night).apply()
    }

    // ===== คะแนนสูงสุด =====
    fun getHighScore(ctx: Context): Int = prefs(ctx).getInt(KEY_HIGH_SCORE, 0)

    fun setHighScore(ctx: Context, score: Int) {
        if (score > getHighScore(ctx)) {
            prefs(ctx).edit().putInt(KEY_HIGH_SCORE, score).apply()
        }
    }

    fun clearHighScore(ctx: Context) {
        prefs(ctx).edit().remove(KEY_HIGH_SCORE).apply()
    }

    // ===== บันทึกและกู้คืนสถานะเกม (Save / Restore Game State) =====

    /** ตรวจสอบว่ามีข้อมูล Save State ที่บันทึกไว้หรือไม่ */
    fun hasSavedGame(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_HAS_SAVED_GAME, false)

    /** บันทึกสถานะเกมปัจจุบัน */
    fun saveGameState(ctx: Context, data: GameStateData) {
        if (data.tubeX.size != NUM_TUBES || data.tubeOffset.size != NUM_TUBES ||
            data.itemType.size != NUM_TUBES || data.itemY.size != NUM_TUBES ||
            data.itemPhase.size != NUM_TUBES) {
            return
        }

        prefs(ctx).edit()
            .putBoolean(KEY_HAS_SAVED_GAME, true)
            .putInt(KEY_STATE_PASSED, data.passed)
            .putInt(KEY_STATE_BONUS, data.bonus)
            .putInt(KEY_STATE_LEVEL, data.level)
            .putInt(KEY_STATE_SCORING_TUBE, data.scoringTube)
            .putFloat(KEY_STATE_BIRD_Y, data.birdY)
            .putFloat(KEY_STATE_VELOCITY, data.velocity)
            .putInt(KEY_STATE_HEARTS, data.hearts)
            .putFloat(KEY_STATE_INVINCIBLE, data.invincible)
            .putFloat(KEY_STATE_RESPAWN_HOVER, data.respawnHover)
            .putString(KEY_STATE_TUBE_X, data.tubeX.joinToString(","))
            .putString(KEY_STATE_TUBE_OFFSET, data.tubeOffset.joinToString(","))
            .putString(KEY_STATE_ITEM_TYPE, data.itemType.joinToString(","))
            .putString(KEY_STATE_ITEM_Y, data.itemY.joinToString(","))
            .putString(KEY_STATE_ITEM_PHASE, data.itemPhase.joinToString(","))
            .putInt(KEY_STATE_TUBE_CUR, data.tubeCur)
            .putInt(KEY_STATE_TUBE_PREV, data.tubePrev)
            .putFloat(KEY_STATE_TUBE_FADE, data.tubeFade)
            .putInt(KEY_STATE_SCREEN_W, data.screenW)
            .putInt(KEY_STATE_SCREEN_H, data.screenH)
            .apply()
    }

    /** อ่านข้อมูล Save State กลับคืนมา (คืนค่า null หากไม่มี หรือข้อมูลไม่สมบูรณ์/ผิดรูปแบบ/ล้มเหลวในการตรวจสอบ) */
    fun loadGameState(ctx: Context): GameStateData? {
        if (!hasSavedGame(ctx)) return null

        val p = prefs(ctx)
        val tubeX = parseFloatArray(p.getString(KEY_STATE_TUBE_X, null)) ?: run { clearSavedGame(ctx); return null }
        val tubeOffset = parseFloatArray(p.getString(KEY_STATE_TUBE_OFFSET, null)) ?: run { clearSavedGame(ctx); return null }
        val itemType = parseIntArray(p.getString(KEY_STATE_ITEM_TYPE, null)) ?: run { clearSavedGame(ctx); return null }
        val itemY = parseFloatArray(p.getString(KEY_STATE_ITEM_Y, null)) ?: run { clearSavedGame(ctx); return null }
        val itemPhase = parseFloatArray(p.getString(KEY_STATE_ITEM_PHASE, null)) ?: run { clearSavedGame(ctx); return null }

        val passed = p.getInt(KEY_STATE_PASSED, -1)
        val bonus = p.getInt(KEY_STATE_BONUS, -1)
        val level = p.getInt(KEY_STATE_LEVEL, 0)
        val scoringTube = p.getInt(KEY_STATE_SCORING_TUBE, -1)
        val hearts = p.getInt(KEY_STATE_HEARTS, 0)
        val tubeCur = p.getInt(KEY_STATE_TUBE_CUR, -1)
        val tubePrev = p.getInt(KEY_STATE_TUBE_PREV, -1)

        val birdY = p.getFloat(KEY_STATE_BIRD_Y, Float.NaN)
        val velocity = p.getFloat(KEY_STATE_VELOCITY, Float.NaN)
        val invincible = p.getFloat(KEY_STATE_INVINCIBLE, Float.NaN)
        val respawnHover = p.getFloat(KEY_STATE_RESPAWN_HOVER, Float.NaN)
        val tubeFade = p.getFloat(KEY_STATE_TUBE_FADE, Float.NaN)

        // Validation ตรวจสอบขอบเขตค่าและ NaN / Infinity
        val isValid = passed >= 0 &&
                bonus >= 0 &&
                level >= 1 &&
                scoringTube >= 0 &&
                hearts in 1..3 &&
                tubeCur in 0..4 &&
                tubePrev in 0..4 &&
                itemType.all { it in 0..3 } &&
                birdY.isFinite() &&
                velocity.isFinite() &&
                invincible.isFinite() && invincible >= 0f &&
                respawnHover.isFinite() && respawnHover >= 0f &&
                tubeFade.isFinite() && tubeFade in 0f..1f

        if (!isValid) {
            clearSavedGame(ctx)
            return null
        }

        return GameStateData(
            passed = passed,
            bonus = bonus,
            level = level,
            scoringTube = scoringTube,
            birdY = birdY,
            velocity = velocity,
            hearts = hearts,
            invincible = invincible,
            respawnHover = respawnHover,
            tubeX = tubeX,
            tubeOffset = tubeOffset,
            itemType = itemType,
            itemY = itemY,
            itemPhase = itemPhase,
            tubeCur = tubeCur,
            tubePrev = tubePrev,
            tubeFade = tubeFade,
            screenW = p.getInt(KEY_STATE_SCREEN_W, 0),
            screenH = p.getInt(KEY_STATE_SCREEN_H, 0)
        )
    }

    /** ลบ Save State เมื่อเกมจบ (Game Over) หรือเริ่มเล่นใหม่ */
    fun clearSavedGame(ctx: Context) {
        prefs(ctx).edit()
            .putBoolean(KEY_HAS_SAVED_GAME, false)
            .remove(KEY_STATE_PASSED)
            .remove(KEY_STATE_BONUS)
            .remove(KEY_STATE_LEVEL)
            .remove(KEY_STATE_SCORING_TUBE)
            .remove(KEY_STATE_BIRD_Y)
            .remove(KEY_STATE_VELOCITY)
            .remove(KEY_STATE_HEARTS)
            .remove(KEY_STATE_INVINCIBLE)
            .remove(KEY_STATE_RESPAWN_HOVER)
            .remove(KEY_STATE_TUBE_X)
            .remove(KEY_STATE_TUBE_OFFSET)
            .remove(KEY_STATE_ITEM_TYPE)
            .remove(KEY_STATE_ITEM_Y)
            .remove(KEY_STATE_ITEM_PHASE)
            .remove(KEY_STATE_TUBE_CUR)
            .remove(KEY_STATE_TUBE_PREV)
            .remove(KEY_STATE_TUBE_FADE)
            .remove(KEY_STATE_SCREEN_W)
            .remove(KEY_STATE_SCREEN_H)
            .apply()
    }

    private fun parseFloatArray(str: String?, expectedSize: Int = NUM_TUBES): FloatArray? {
        if (str.isNullOrBlank()) return null
        val parts = str.split(",")
        if (parts.size != expectedSize) return null
        val arr = FloatArray(expectedSize)
        for (i in 0 until expectedSize) {
            val v = parts[i].toFloatOrNull() ?: return null
            if (!v.isFinite()) return null
            arr[i] = v
        }
        return arr
    }

    private fun parseIntArray(str: String?, expectedSize: Int = NUM_TUBES): IntArray? {
        if (str.isNullOrBlank()) return null
        val parts = str.split(",")
        if (parts.size != expectedSize) return null
        val arr = IntArray(expectedSize)
        for (i in 0 until expectedSize) {
            arr[i] = parts[i].toIntOrNull() ?: return null
        }
        return arr
    }
}
