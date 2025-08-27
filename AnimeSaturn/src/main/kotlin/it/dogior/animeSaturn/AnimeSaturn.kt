package it.brusus.animesaturn

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.mvvm.safeApiCall
import org.jsoup.nodes.Element

class AnimeSaturn : MainAPI() {
    override var mainUrl = "https://www.animesaturn.cx"
    override var name = "AnimeSaturn"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Anime)
    override val hasQuickSearch = true

    // Sezione "Nuove aggiunte"
    override val mainPage = mainPageOf(
        Pair("newest", "Nuove aggiunte")
    )

    // Ricerca: il sito non espone chiaramente una endpoint di search pubblico;
    // come fallback, usiamo /filter e filtriamo client-side per titolo contiene query (pagina 1).
    // In seguito possiamo migliorare se scopriamo una endpoint server-side stabile.
    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/filter").document
        return doc.select("a:has(img)").mapNotNull { it.toSearchResponse() }
            .filter { it.name.contains(query, ignoreCase = true) }
    }

    // Home -> "Nuove aggiunte"
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (request.data == "newest") "$mainUrl/newest?page=$page" else mainUrl
        val doc = app.get(url).document
        val items = doc.select("a:has(img)").mapNotNull { it.toSearchResponse() }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResponse(): AnimeSearchResponse? {
        val link = this.attr("href") ?: return null
        val img = this.selectFirst("img")
        val title = img?.attr("alt")?.ifBlank { text() } ?: text()
        val poster = img?.absUrl("src")
        if (title.isNullOrBlank()) return null
        return newAnimeSearchResponse(title, fixUrl(link)) {
            this.posterUrl = poster
        }
    }

    // Pagina anime (scheda con "Lista Episodi")
    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document

        val title = doc.selectFirst("h5,h4,h3,h2,h1")?.text()?.takeIf { it.isNotBlank() }
            ?: throw ErrorLoadingException("Titolo mancante")
        val poster = doc.selectFirst("img[alt*=$title], .cover img, .poster img")?.absUrl("src")
        val plot = doc.selectFirst(".description, .trama, p:contains(Trama)")?.text()
            ?: doc.selectFirst("main p")?.text()
        val tags = doc.select("a[href*=/genre], a[href*=/tag]").map { it.text() }.takeIf { it.isNotEmpty() }

        // Episodi: link tipo /ep/Qualcosa-ep-1
        val eps = doc.select("a:matchesOwn(^\\s*Episodio\\s+\\d+)").mapIndexed { index, a ->
            val epUrl = a.absUrl("href")
            val epNum = Regex("(\\d+)").find(a.text())?.groupValues?.get(1)?.toIntOrNull() ?: (index + 1)
            newEpisode(epUrl) {
                this.name = a.text()
                this.episode = epNum
            }
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.plot = plot
            this.tags = tags
            this.episodes = eps
        }
    }

    // Caricamento link: entra nella pagina episodio -> "Guarda lo streaming"
    // quindi nella pagina /watch?file=... e passa tutti gli embed agli estrattori
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // data è l'URL dell'episodio (/ep/..)
        val epDoc = app.get(data).document
        val watchHref = epDoc.selectFirst("a:matchesOwn(^\\s*Guarda\\s+lo\\s+streaming)")?.absUrl("href")
            ?: return false

        val watchDoc = app.get(watchHref).document

        // 1) link “Download da <host>” (es. listeamed / VidGuard)
        watchDoc.select("a[href]").forEach { a ->
            val url = a.absUrl("href")
            if (url.contains("listeamed") ||
                url.contains("vidguard") ||
                url.contains("streamtape") ||
                url.contains("dood") ||
                url.contains("filemoon")
            ) {
                safeApiCall {
                    loadExtractor(url, mainUrl, subtitleCallback, callback)
                }
            }
        }

        // 2) fallback: iframes
        watchDoc.select("iframe[src]").forEach { i ->
            val url = i.absUrl("src")
            safeApiCall {
                loadExtractor(url, mainUrl, subtitleCallback, callback)
            }
        }

        return true
    }
}

