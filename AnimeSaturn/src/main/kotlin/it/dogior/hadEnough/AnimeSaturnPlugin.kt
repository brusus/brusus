package it.dogior.hadEnough

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AnimeSaturnPlugin : Plugin() {
    override fun load(context: Context) {
        // Metodo di BasePlugin -> disponibile dentro la classe
        registerMainAPI(AnimeSaturn())
        // in caso di dubbi:
        // this.registerMainAPI(AnimeSaturn())
    }
}
