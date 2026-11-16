package com.kostasdrakonakis.flappybird

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class HighScoreActivity : Activity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SettingsHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_score)

        val scoreText = findViewById<TextView>(R.id.highScoreText)
        val clearButton = findViewById<Button>(R.id.btnClearHighScore)
        val backButton = findViewById<Button>(R.id.btnBack)

        updateHighScore(scoreText)

        clearButton.setOnClickListener {
            showClearConfirmation(scoreText)
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun updateHighScore(scoreText: TextView) {
        scoreText.text = GamePrefs.getHighScore(this).toString()
    }

    private fun showClearConfirmation(scoreText: TextView) {
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_high_score_title))
            .setMessage(getString(R.string.clear_high_score_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.clear)) { _, _ ->
                GamePrefs.clearHighScore(this)
                updateHighScore(scoreText)
            }
            .show()
    }
}