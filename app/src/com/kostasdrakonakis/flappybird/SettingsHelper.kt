package com.kostasdrakonakis.flappybird

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object SettingsHelper {

    fun wrap(base: Context): Context {
        val config = Configuration(base.resources.configuration)

        GamePrefs.getLanguage(base)?.let { lang ->
            config.setLocale(Locale.forLanguageTag(lang))
        }

        val night = if (GamePrefs.isNightMode(base)) Configuration.UI_MODE_NIGHT_YES
        else Configuration.UI_MODE_NIGHT_NO
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night

        return base.createConfigurationContext(config)
    }

    fun currentKey(ctx: Context): String =
        "${GamePrefs.getLanguage(ctx)}|${GamePrefs.isNightMode(ctx)}"
}