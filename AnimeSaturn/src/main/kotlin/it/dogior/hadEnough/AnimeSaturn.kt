package it.dogior.hadEnough

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class AnimeSaturn : MainAPI() {
    override var mainUrl = "https://www.animesaturn.cx"
    override var name = "AnimeSaturn"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Anime)
    override val hasQuickSearch = true

    override val mainPage = mainPageOf(
        "newest" to "Nuove aggiunte"
    )

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/filter").document
        return doc.select("a:has(img)").mapNotNull { it.toSearchResponse() }
            .filter { it.name.contains(query, ignoreCase = true) }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (request.data == "newest") "$mainUrl/newest?page=$page" else mainUrl
        val doc = app.get(url).document
        val items = doc.select("a:has(img)").mapNotNull { it.toSearchResponse() }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResponse(): AnimeSearchResponse? {
        val link = attr("href") ?: return null
        val img = selectFirst("img")
        val title = img?.attr("alt")?.ifBlank { text() } ?: text()
        val poster = img?.absUrl("src")
        if (title.isNullOrBlank()) return null
        return newAnimeSearchResponse(title, fixUrl(link)) {
            this.posterUrl = poster
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document

        val title = doc.selectFirst("h1,h2,h3,h4,h5")?.text()
            ?: throw ErrorLoadingException("Titolo mancante")

        val poster = doc.selectFirst("img[alt*=$title], .cover img, .poster img")?.absUrl("src")
        val plot = doc.selectFirst(".description, .trama, p:contains(Trama)")?.text()
            ?: doc.selectFirst("main p")?.text()
        val tags = doc.select("a[href*=/genre], a[href*=/tag]").map { it.text() }
            .takeIf { it.isNotEmpty() }

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
            addEpisodes(DubStatus.Subbed, eps) // mappa per stato (Subbed/Dubbed)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val epDoc = app.get(data).document
        val watchHref = epDoc.selectFirst("a:matchesOwn(^\\s*Guarda\\s+lo\\s+streaming)")?.absUrl("href")
            ?: return false

        val watchDoc = app.get(watchHref).document

        // Link diretti presenti nella pagina watch
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
        // Fallback: iframe embed
        watchDoc.select("iframe[src]").forEach { i ->
            val url = i.absUrl("src")
            safeApiCall {
                loadExtractor(url, mainUrl, subtitleCallback, callback)
            }
        }

        return true
    }
}
