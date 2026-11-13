/*
 * Copyright 2018 Konstantinos Drakonakis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kostasdrakonakis.flappybird

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class FlappyBird(private val characterIndex: Int = 0) : ApplicationAdapter() {

    private lateinit var batch: SpriteBatch
    private lateinit var background: Texture
    private lateinit var gameOver: Texture
    private lateinit var birds: Array<Texture>
    private lateinit var coinTex: Texture
    private lateinit var obstacleTex: Texture
    private lateinit var heartFull: Texture
    private lateinit var heartEmpty: Texture
    private lateinit var topTubeRectangles: Array<Rectangle>
    private lateinit var bottomTubeRectangles: Array<Rectangle>
    private lateinit var birdCircle: Circle
    private val hitCircle = Circle()
    private val itemCircle = Circle()
    private lateinit var font: BitmapFont
    private lateinit var topTube: Texture
    private lateinit var bottomTube: Texture
    private lateinit var random: Random
    private val layout = GlyphLayout()

    private var flapState = 0
    private var flapTimer = 0f
    private var birdY: Float = 0f
    private var velocity: Float = 0f
    private var passed: Int = 0      // จำนวนท่อที่ผ่าน (ใช้คิดด่าน)
    private var bonus: Int = 0       // คะแนนโบนัสจากเหรียญ
    private val score: Int get() = passed + bonus
    private var level: Int = 1
    private var scoringTube: Int = 0
    private var gameState: Int = STATE_READY
    private val numberOfTubes: Int = 4
    private var gdxHeight: Int = 0
    private var gdxWidth: Int = 0

    // ขนาดทุกอย่างอิงจากความสูงจอ (ค่าเดิมออกแบบไว้สำหรับจอสูง ~1920px)
    private var unit = 1f
    private var gap = 0f
    private var tubeW = 0f
    private var tubeH = 0f
    private var birdW = 0f
    private var birdH = 0f
    private var coinSize = 0f
    private var obstacleSize = 0f

    private val tubeX = FloatArray(numberOfTubes)
    private val tubeOffset = FloatArray(numberOfTubes)
    private var distanceBetweenTubes: Float = 0f

    // ไอเทมระหว่างท่อ: 1 ช่องต่อท่อ 1 ต้น (อยู่กึ่งกลางระหว่างท่อนี้กับท่อก่อนหน้า)
    private val itemType = IntArray(numberOfTubes)
    private val itemY = FloatArray(numberOfTubes)
    private val itemPhase = FloatArray(numberOfTubes)

    // หัวใจ / ภูมิคุ้มกันชั่วคราวหลังโดนชน
    private var hearts = MAX_HEARTS
    private var invincible = 0f

    // ข้อความลอย (+3 / OUCH!)
    private var popupText = ""
    private var popupTimer = 0f
    private var popupX = 0f
    private var popupY = 0f
    private val popupColor = Color(Color.WHITE)

    // ระบบด่าน
    private val bgTint = Color(Color.WHITE)
    private val tubeTint = Color(Color.WHITE)
    private var bannerTimer = 0f
    private var gameOverTimer = 0f
    private var blinkTimer = 0f
    private var time = 0f

    override fun create() {
        batch = SpriteBatch()
        background = Texture("bg.png")
        gameOver = Texture("gameover.png")
        coinTex = Texture("coin.png")
        obstacleTex = Texture("obstacle.png")
        heartFull = Texture("heart_full.png")
        heartEmpty = Texture("heart_empty.png")
        birdCircle = Circle()
        font = BitmapFont()
        font.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        val skin = Characters.list[characterIndex]
        birds = arrayOf(Texture(skin.frameA), Texture(skin.frameB))

        gdxHeight = Gdx.graphics.height
        gdxWidth = Gdx.graphics.width
        unit = gdxHeight / REFERENCE_HEIGHT

        topTube = Texture("toptube.png")
        bottomTube = Texture("bottomtube.png")
        random = Random()

        gap = GAP * unit
        tubeW = topTube.width * unit
        tubeH = topTube.height * unit
        birdW = birds[0].width * unit
        birdH = birds[0].height * unit
        coinSize = coinTex.width * unit
        obstacleSize = obstacleTex.width * unit
        distanceBetweenTubes = gdxWidth * 3f / 4f

        topTubeRectangles = Array(numberOfTubes) { Rectangle() }
        bottomTubeRectangles = Array(numberOfTubes) { Rectangle() }

        resetGame()
    }

    override fun render() {
        // ทำให้ความเร็วเท่ากันทุกเครื่อง ไม่ว่าจอจะ 60Hz หรือ 120Hz
        val dt = Gdx.graphics.deltaTime
        val f = minOf(dt * 60f, 3f)

        updateTint(f)
        update(dt, f)

        batch.begin()
        batch.color = bgTint
        batch.draw(background, 0f, 0f, gdxWidth.toFloat(), gdxHeight.toFloat())

        batch.color = tubeTint
        for (i in 0 until numberOfTubes) {
            batch.draw(topTube, tubeX[i], topTubeRectangles[i].y, tubeW, tubeH)
            batch.draw(bottomTube, tubeX[i], bottomTubeRectangles[i].y, tubeW, tubeH)
        }

        batch.color = Color.WHITE
        drawItems()

        // นกกะพริบตอนมีภูมิคุ้มกัน
        val blinkOff = invincible > 0f && ((invincible * 10f).toInt() % 2 == 0)
        batch.setColor(1f, 1f, 1f, if (blinkOff) 0.35f else 1f)
        batch.draw(birds[flapState], gdxWidth / 2f - birdW / 2f, birdY, birdW, birdH)
        batch.setColor(1f, 1f, 1f, 1f)

        drawHud()
        batch.end()
    }

    private fun update(dt: Float, f: Float) {
        time += dt
        flapTimer += dt
        if (flapTimer > 0.1f) {
            flapTimer = 0f
            flapState = 1 - flapState
        }
        if (bannerTimer > 0f) bannerTimer -= dt
        if (popupTimer > 0f) popupTimer -= dt
        if (invincible > 0f) invincible -= dt
        blinkTimer += dt

        when (gameState) {
            STATE_READY -> {
                if (Gdx.input.justTouched()) {
                    velocity = JUMP * unit
                    gameState = STATE_PLAYING
                }
            }

            STATE_PLAYING -> {
                if (Gdx.input.justTouched()) velocity = JUMP * unit

                val speed = TUBE_VELOCITY * unit * speedMultiplier()
                for (i in 0 until numberOfTubes) {
                    if (tubeX[i] < -tubeW) {
                        tubeX[i] += numberOfTubes * distanceBetweenTubes
                        tubeOffset[i] = randomOffset()
                        rollItem(i)
                    } else {
                        tubeX[i] -= speed * f
                    }
                    updateTubeRectangles(i)
                }

                velocity += GRAVITY * unit * f
                birdY -= velocity * f
                val maxY = gdxHeight - birdH
                if (birdY > maxY) {
                    birdY = maxY
                    if (velocity < 0f) velocity = 0f
                }
                if (birdY <= 0f) {
                    birdY = 0f
                    endGame()
                    return
                }

                // นับคะแนนเมื่อผ่านท่อ
                if (tubeX[scoringTube] + tubeW < gdxWidth / 2f - birdW / 2f) {
                    passed++
                    scoringTube = (scoringTube + 1) % numberOfTubes
                    checkLevelUp()
                }

                // ชนท่อ = จบเกมทันที
                birdCircle.set(gdxWidth / 2f, birdY + birdH / 2f, birdW / 2f)
                for (i in 0 until numberOfTubes) {
                    if (Intersector.overlaps(birdCircle, topTubeRectangles[i]) ||
                        Intersector.overlaps(birdCircle, bottomTubeRectangles[i])) {
                        endGame()
                        return
                    }
                }

                checkItems()
            }

            STATE_GAME_OVER -> {
                gameOverTimer += dt
                // รอสั้น ๆ ก่อนรับการแตะ ป้องกันกดรีสตาร์ทโดยไม่ตั้งใจ
                if (gameOverTimer > RESTART_DELAY && Gdx.input.justTouched()) {
                    resetGame()
                }
            }
        }
    }

    // ---------- ไอเทม: เหรียญ / สิ่งกีดขวาง ----------

    private fun itemX(i: Int) = tubeX[i] - distanceBetweenTubes / 2f + tubeW / 2f

    private fun itemYNow(i: Int): Float =
        if (itemType[i] == ITEM_OBSTACLE) itemY[i] + sin(time * 2.2f + itemPhase[i]) * OBSTACLE_BOB * unit
        else itemY[i]

    /** สุ่มไอเทมให้ช่องที่ i (ตั้งแต่ด่าน ITEMS_FROM_LEVEL เป็นต้นไป) */
    private fun rollItem(i: Int) {
        itemType[i] = ITEM_NONE
        if (level < ITEMS_FROM_LEVEL) return

        val obstacleChance = minOf(0.25f + (level - ITEMS_FROM_LEVEL) * 0.05f, 0.45f)
        val r = random.nextFloat()
        itemType[i] = when {
            r < obstacleChance -> ITEM_OBSTACLE
            r < obstacleChance + COIN_CHANCE -> ITEM_COIN
            else -> ITEM_NONE
        }
        if (itemType[i] == ITEM_NONE) return

        // วางไว้บนเส้นทางระหว่างช่องว่างของท่อก่อนหน้ากับท่อนี้ เพื่อให้เก็บ/หลบได้
        val prev = (i - 1 + numberOfTubes) % numberOfTubes
        val c1 = gdxHeight / 2f + tubeOffset[prev]
        val c2 = gdxHeight / 2f + tubeOffset[i]
        val t = 0.3f + random.nextFloat() * 0.4f
        val y = c1 + (c2 - c1) * t + (random.nextFloat() - 0.5f) * gap * 0.4f
        val margin = 200f * unit
        itemY[i] = y.coerceIn(margin, gdxHeight - margin)
        itemPhase[i] = random.nextFloat() * 6.28f
    }

    /** ตอนขึ้นด่านที่เริ่มมีไอเทม ให้ท่อที่ยังอยู่นอกจอทางขวามีไอเทมทันที */
    private fun populateOffscreenItems() {
        for (i in 0 until numberOfTubes) {
            if (itemX(i) > gdxWidth + 150f * unit) rollItem(i)
        }
    }

    private fun checkItems() {
        val bx = gdxWidth / 2f
        val by = birdY + birdH / 2f
        hitCircle.set(bx, by, birdH * 0.42f)

        for (i in 0 until numberOfTubes) {
            val type = itemType[i]
            if (type == ITEM_NONE) continue
            val ix = itemX(i)
            if (ix < bx - 300f * unit || ix > bx + 300f * unit) continue
            val iy = itemYNow(i)

            if (type == ITEM_COIN) {
                itemCircle.set(ix, iy, coinSize * 0.5f)
                if (Intersector.overlaps(birdCircle, itemCircle)) {
                    itemType[i] = ITEM_NONE
                    bonus += COIN_VALUE
                    showPopup("+$COIN_VALUE", Color(1f, 0.9f, 0.2f, 1f))
                }
            } else if (type == ITEM_OBSTACLE && invincible <= 0f) {
                itemCircle.set(ix, iy, obstacleSize * 0.38f)
                if (Intersector.overlaps(hitCircle, itemCircle)) {
                    itemType[i] = ITEM_NONE
                    hearts--
                    invincible = INVINCIBLE_TIME
                    showPopup("OUCH!", Color(1f, 0.35f, 0.35f, 1f))
                    if (hearts <= 0) {
                        endGame()
                        return
                    }
                }
            }
        }
    }

    private fun showPopup(text: String, color: Color) {
        popupText = text
        popupColor.set(color)
        popupTimer = POPUP_TIME
        popupX = gdxWidth / 2f
        popupY = birdY + birdH + 60f * unit
    }

    private fun drawItems() {
        for (i in 0 until numberOfTubes) {
            val type = itemType[i]
            if (type == ITEM_NONE) continue
            val ix = itemX(i)
            if (ix < -obstacleSize || ix > gdxWidth + obstacleSize) continue
            val iy = itemYNow(i)

            if (type == ITEM_COIN) {
                // เหรียญหมุน: บีบความกว้างตามเวลา
                val w = coinSize * maxOf(0.2f, abs(cos(time * 5f + itemPhase[i])))
                batch.draw(coinTex, ix - w / 2f, iy - coinSize / 2f, w, coinSize)
            } else {
                val w = obstacleSize
                batch.draw(obstacleTex, ix - w / 2f, iy - w / 2f, w / 2f, w / 2f, w, w,
                        1f, 1f, time * 120f + itemPhase[i] * 57f,
                        0, 0, obstacleTex.width, obstacleTex.height, false, false)
            }
        }
    }

    // ---------- ระบบด่าน ----------

    private fun levelOf(p: Int) = p / SCORE_PER_LEVEL + 1

    private fun speedMultiplier(): Float =
        minOf(1f + (level - 1) * SPEED_STEP, MAX_SPEED_MULTIPLIER)

    private fun checkLevelUp() {
        val newLevel = levelOf(passed)
        if (newLevel != level) {
            level = newLevel
            bannerTimer = BANNER_TIME
            if (level >= ITEMS_FROM_LEVEL) populateOffscreenItems()
        }
    }

    /** ค่อย ๆ เปลี่ยนสีพื้นหลัง/ท่อไปหาสีของด่านปัจจุบัน */
    private fun updateTint(f: Float) {
        val p = PALETTES[(level - 1) % PALETTES.size]
        val t = minOf(TINT_SPEED * f, 1f)
        bgTint.lerp(p.bg, t)
        tubeTint.lerp(p.tube, t)
    }

    // ---------- สถานะเกม ----------

    private fun endGame() {
        gameState = STATE_GAME_OVER
        gameOverTimer = 0f
    }

    private fun resetGame() {
        passed = 0
        bonus = 0
        level = 1
        hearts = MAX_HEARTS
        invincible = 0f
        popupTimer = 0f
        scoringTube = 0
        velocity = 0f
        bannerTimer = 0f
        flapState = 0
        bgTint.set(PALETTES[0].bg)
        tubeTint.set(PALETTES[0].tube)
        birdY = gdxHeight / 2f - birdH / 2f

        for (i in 0 until numberOfTubes) {
            tubeOffset[i] = randomOffset()
            tubeX[i] = gdxWidth / 2f - tubeW / 2f + gdxWidth.toFloat() + i * distanceBetweenTubes
            itemType[i] = ITEM_NONE
            updateTubeRectangles(i)
        }
        gameState = STATE_READY
    }

    private fun randomOffset() = (random.nextFloat() - 0.5f) * (gdxHeight.toFloat() - gap - 200f * unit)

    private fun updateTubeRectangles(i: Int) {
        val topY = gdxHeight / 2f + gap / 2f + tubeOffset[i]
        val bottomY = gdxHeight / 2f - gap / 2f - tubeH + tubeOffset[i]
        topTubeRectangles[i].set(tubeX[i], topY, tubeW, tubeH)
        bottomTubeRectangles[i].set(tubeX[i], bottomY, tubeW, tubeH)
    }

    // ---------- วาดข้อความ / HUD ----------

    private fun drawText(text: String, centerX: Float, y: Float, scale: Float, color: Color = Color.WHITE) {
        font.data.setScale(scale * unit)
        font.color = color
        layout.setText(font, text)
        font.draw(batch, layout, centerX - layout.width / 2f, y)
    }

    private fun drawCentered(text: String, y: Float, scale: Float, color: Color = Color.WHITE) =
        drawText(text, gdxWidth / 2f, y, scale, color)

    private fun drawHearts() {
        val hw = heartFull.width * unit
        val hh = heartFull.height * unit
        val startX = 36f * unit
        val y = gdxHeight - hh - 60f * unit
        for (i in 0 until MAX_HEARTS) {
            val tex = if (i < hearts) heartFull else heartEmpty
            batch.draw(tex, startX + i * (hw + 12f * unit), y, hw, hh)
        }
    }

    private fun drawHud() {
        val top = gdxHeight - 80f * unit

        drawHearts()

        if (gameState != STATE_GAME_OVER) {
            drawCentered(score.toString(), top, 9f)
            drawCentered("LEVEL $level", top - 150f * unit, 3.5f)
        }

        if (gameState == STATE_READY) {
            drawCentered("TAP TO START", gdxHeight * 0.3f, 4f)
        }

        // ป้ายขึ้นด่านใหม่
        if (gameState == STATE_PLAYING && bannerTimer > 0f) {
            val a = minOf(bannerTimer / BANNER_TIME * 2f, 1f)
            drawCentered("LEVEL $level", gdxHeight * 0.7f, 8f, Color(1f, 0.9f, 0.2f, a))
            val sub = if (level == ITEMS_FROM_LEVEL) "COINS & SPIKES!" else "SPEED UP!"
            drawCentered(sub, gdxHeight * 0.7f - 160f * unit, 4f, Color(1f, 1f, 1f, a))
        }

        // ข้อความลอย +3 / OUCH!
        if (popupTimer > 0f && gameState == STATE_PLAYING) {
            val p = 1f - popupTimer / POPUP_TIME
            val c = Color(popupColor.r, popupColor.g, popupColor.b, 1f - p)
            drawText(popupText, popupX, popupY + p * 120f * unit, 5f, c)
        }

        if (gameState == STATE_GAME_OVER) {
            val gw = gameOver.width * unit
            val gh = gameOver.height * unit
            batch.draw(gameOver, gdxWidth / 2f - gw / 2f, gdxHeight * 0.66f, gw, gh)
            drawCentered("SCORE  $score", gdxHeight * 0.58f, 6f)
            drawCentered("LEVEL  $level", gdxHeight * 0.58f - 130f * unit, 4f)
            if (gameOverTimer > RESTART_DELAY && (blinkTimer % 1f) < 0.65f) {
                drawCentered("TAP TO RESTART", gdxHeight * 0.3f, 4f)
            }
        }
    }

    override fun dispose() {
        batch.dispose()
        background.dispose()
        gameOver.dispose()
        topTube.dispose()
        bottomTube.dispose()
        coinTex.dispose()
        obstacleTex.dispose()
        heartFull.dispose()
        heartEmpty.dispose()
        birds.forEach { it.dispose() }
        font.dispose()
    }

    private class Palette(val bg: Color, val tube: Color)

    companion object {
        private const val STATE_READY = 0
        private const val STATE_PLAYING = 1
        private const val STATE_GAME_OVER = 2

        private const val ITEM_NONE = 0
        private const val ITEM_COIN = 1
        private const val ITEM_OBSTACLE = 2

        private const val REFERENCE_HEIGHT = 1920f
        private const val GRAVITY = 2f
        private const val JUMP = -30f
        private const val TUBE_VELOCITY = 4f
        private const val GAP = 800f

        private const val SCORE_PER_LEVEL = 5       // ทุก 5 ท่อ = 1 ด่าน
        private const val SPEED_STEP = 0.15f        // เร็วขึ้น 15% ต่อด่าน
        private const val MAX_SPEED_MULTIPLIER = 2.5f
        private const val TINT_SPEED = 0.03f
        private const val BANNER_TIME = 2f
        private const val RESTART_DELAY = 0.6f

        private const val MAX_HEARTS = 3            // หัวใจเริ่มต้น
        private const val ITEMS_FROM_LEVEL = 2      // เหรียญ/สิ่งกีดขวางเริ่มที่ด่านนี้
        private const val COIN_VALUE = 3            // คะแนนต่อเหรียญ
        private const val COIN_CHANCE = 0.40f       // โอกาสมีเหรียญ (ต่อช่อง)
        private const val INVINCIBLE_TIME = 1.5f    // วินาทีที่ไม่โดนซ้ำหลังโดนชน
        private const val OBSTACLE_BOB = 70f        // ระยะที่สิ่งกีดขวางลอยขึ้นลง
        private const val POPUP_TIME = 0.9f

        // สีของแต่ละด่าน (คูณทับภาพพื้นหลัง/ท่อ) วนซ้ำเมื่อเกินด่านสุดท้าย
        private val PALETTES = arrayOf(
            Palette(Color(1.00f, 1.00f, 1.00f, 1f), Color(1.00f, 1.00f, 1.00f, 1f)), // 1 ฟ้าปกติ
            Palette(Color(1.00f, 0.80f, 0.55f, 1f), Color(1.00f, 0.90f, 0.60f, 1f)), // 2 ส้มพระอาทิตย์ตก
            Palette(Color(0.85f, 0.60f, 1.00f, 1f), Color(0.80f, 0.70f, 1.00f, 1f)), // 3 ม่วง
            Palette(Color(1.00f, 0.50f, 0.55f, 1f), Color(1.00f, 0.70f, 0.60f, 1f)), // 4 แดงอมชมพู
            Palette(Color(0.40f, 0.45f, 0.75f, 1f), Color(0.55f, 0.65f, 0.80f, 1f))  // 5 กลางคืน
        )
    }
}
