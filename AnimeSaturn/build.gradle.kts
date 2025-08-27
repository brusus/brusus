plugins {
    id("com.android.library")
    kotlin("android")
    id("com.lagradost.cloudstream3.gradle")
}

cloudstream {
    language = "it"
    description = "Anime da AnimeSaturn (SUB ITA & ITA)."
    authors = listOf("brusus")
    iconUrl = "https://www.animesaturn.cx/immagini/favicon-32x32.png"
    tvTypes = listOf("Anime")
}

android {
    // Namespace UNICO per il modulo, ma può differire dal package Kotlin
    namespace = "it.dogior.hadEnough.animesaturn"
    compileSdk = 34
    defaultConfig { minSdk = 21 }
}
