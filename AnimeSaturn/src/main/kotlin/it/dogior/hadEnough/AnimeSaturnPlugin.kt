@file:Suppress("unused")

package it.brusus.animesaturn

import android.content.Context
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.PluginManager.registerMainAPI

class AnimeSaturnPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(AnimeSaturn())
    }
}
