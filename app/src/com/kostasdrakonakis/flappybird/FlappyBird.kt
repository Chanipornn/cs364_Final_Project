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

class FlappyBird(private val characterIndex: Int = 0) : ApplicationAdapter() {

    private lateinit var batch: SpriteBatch
    private lateinit var background: Texture
    private lateinit var gameOver: Texture
    private lateinit var birds: Array<Texture>
    private lateinit var topTubeRectangles: Array<Rectangle>
    private lateinit var bottomTubeRectangles: Array<Rectangle>
    private lateinit var birdCircle: Circle
    private lateinit var font: BitmapFont
    private lateinit var topTube: Texture
    private lateinit var bottomTube: Texture
    private lateinit var random: Random
    private val layout = GlyphLayout()

    private var flapState = 0
    private var flapTimer = 0f
    private var birdY: Float = 0f
    private var velocity: Float = 0f
    private var score: Int = 0
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

    private val tubeX = FloatArray(numberOfTubes)
    private val tubeOffset = FloatArray(numberOfTubes)
    private var distanceBetweenTubes: Float = 0f

    // ระบบด่าน
    private val bgTint = Color(Color.WHITE)
    private val tubeTint = Color(Color.WHITE)
    private var bannerTimer = 0f
    private var gameOverTimer = 0f
    private var blinkTimer = 0f

    override fun create() {
        batch = SpriteBatch()
        background = Texture("bg.png")
        gameOver = Texture("gameover.png")
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
        batch.draw(birds[flapState], gdxWidth / 2f - birdW / 2f, birdY, birdW, birdH)

        drawHud()
        batch.end()
    }

    private fun update(dt: Float, f: Float) {
        flapTimer += dt
        if (flapTimer > 0.1f) {
            flapTimer = 0f
            flapState = 1 - flapState
        }
        if (bannerTimer > 0f) bannerTimer -= dt
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
                    score++
                    scoringTube = (scoringTube + 1) % numberOfTubes
                    checkLevelUp()
                }

                // ชนท่อ
                birdCircle.set(gdxWidth / 2f, birdY + birdH / 2f, birdW / 2f)
                for (i in 0 until numberOfTubes) {
                    if (Intersector.overlaps(birdCircle, topTubeRectangles[i]) ||
                        Intersector.overlaps(birdCircle, bottomTubeRectangles[i])) {
                        endGame()
                        return
                    }
                }
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

    // ---------- ระบบด่าน ----------

    private fun levelOf(s: Int) = s / SCORE_PER_LEVEL + 1

    private fun speedMultiplier(): Float =
        minOf(1f + (level - 1) * SPEED_STEP, MAX_SPEED_MULTIPLIER)

    private fun checkLevelUp() {
        val newLevel = levelOf(score)
        if (newLevel != level) {
            level = newLevel
            bannerTimer = BANNER_TIME
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
        score = 0
        level = 1
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

    // ---------- วาดข้อความ ----------

    private fun drawCentered(text: String, y: Float, scale: Float, color: Color = Color.WHITE) {
        font.data.setScale(scale * unit)
        font.color = color
        layout.setText(font, text)
        font.draw(batch, layout, (gdxWidth - layout.width) / 2f, y)
    }

    private fun drawHud() {
        val top = gdxHeight - 80f * unit

        // คะแนน + ด่าน
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
            drawCentered("SPEED UP!", gdxHeight * 0.7f - 160f * unit, 4f, Color(1f, 1f, 1f, a))
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
        birds.forEach { it.dispose() }
        font.dispose()
    }

    private class Palette(val bg: Color, val tube: Color)

    companion object {
        private const val STATE_READY = 0
        private const val STATE_PLAYING = 1
        private const val STATE_GAME_OVER = 2

        private const val REFERENCE_HEIGHT = 1920f
        private const val GRAVITY = 2f
        private const val JUMP = -30f
        private const val TUBE_VELOCITY = 4f
        private const val GAP = 800f

        private const val SCORE_PER_LEVEL = 5       // ทุก 5 คะแนน = 1 ด่าน
        private const val SPEED_STEP = 0.15f        // เร็วขึ้น 15% ต่อด่าน
        private const val MAX_SPEED_MULTIPLIER = 2.5f
        private const val TINT_SPEED = 0.03f
        private const val BANNER_TIME = 2f
        private const val RESTART_DELAY = 0.6f

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
