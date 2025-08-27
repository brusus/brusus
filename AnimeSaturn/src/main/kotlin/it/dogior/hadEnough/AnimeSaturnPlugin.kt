package it.dogior.hadEnough

import android.content.Context
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.PluginManager

class AnimeSaturnPlugin : Plugin() {
    override fun load(context: Context) {
        PluginManager.registerMainAPI(AnimeSaturn())
    }
}
