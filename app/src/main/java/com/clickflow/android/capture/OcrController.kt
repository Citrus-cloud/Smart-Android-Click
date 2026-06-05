package com.clickflow.android.capture

/**
 * Step 71 — OCR controller: orchestrates [OcrProvider] + text-search logic.
 *
 * Runs OCR via the injected [provider] and exposes helper methods to find
 * text regions matching a query. Pure Kotlin, no Android imports, no tap dispatch.
 *
 * @param provider  The OCR engine (real or stub).
 */
class OcrController(private val provider: OcrProvider) {

    /**
     * Run OCR and return the full [OcrResult].
     *
     * @param candidates  Optional hint regions to focus recognition on.
     */
    fun recognize(candidates: List<CaptureRegion> = emptyList()): OcrResult =
        provider.recognize(candidates)

    /**
     * Find all regions whose [OcrTextRegion.text] *contains* [query].
     *
     * @param query          The substring to search for.
     * @param result         The [OcrResult] to search in.
     * @param caseSensitive  If false (default), comparison is case-insensitive.
     * @return Matching regions in their original order.
     */
    fun findText(
        query: String,
        result: OcrResult,
        caseSensitive: Boolean = false
    ): List<OcrTextRegion> {
        if (query.isEmpty()) return emptyList()
        return result.regions.filter { region ->
            if (caseSensitive) region.text.contains(query)
            else region.text.contains(query, ignoreCase = true)
        }
    }

    /**
     * Find all regions whose [OcrTextRegion.text] *equals* [query].
     *
     * @param query          The exact string to match.
     * @param result         The [OcrResult] to search in.
     * @param caseSensitive  If false (default), comparison is case-insensitive.
     * @return Matching regions in their original order.
     */
    fun findExact(
        query: String,
        result: OcrResult,
        caseSensitive: Boolean = false
    ): List<OcrTextRegion> {
        if (query.isEmpty()) return emptyList()
        return result.regions.filter { region ->
            if (caseSensitive) region.text == query
            else region.text.equals(query, ignoreCase = true)
        }
    }

    /**
     * Return the best (highest-confidence) matching region for [query],
     * or null when no match is found.
     *
     * @param query          Substring to search for.
     * @param result         The [OcrResult] to search in.
     * @param caseSensitive  If false (default), case-insensitive.
     */
    fun bestMatch(
        query: String,
        result: OcrResult,
        caseSensitive: Boolean = false
    ): OcrTextRegion? =
        findText(query, result, caseSensitive).maxByOrNull { it.confidence }
}
