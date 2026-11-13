package com.kostasdrakonakis.flappybird

import androidx.annotation.StringRes

data class CharacterSkin(
    @StringRes val nameRes: Int,
    val frameA: String,
    val frameB: String
)

object Characters {
    val list = listOf(
        CharacterSkin(R.string.char_classic, "bird.png", "bird2.png"),
        CharacterSkin(R.string.char_red, "char1_a.png", "char1_b.png"),
        CharacterSkin(R.string.char_blue, "char2_a.png", "char2_b.png")
    )
}
