package com.clickflow.android.capture

/**
 * Step 70 — Image-target scenario controller.
 *
 * Wires a [TemplateManager] and a [TemplateMatcher] into a single
 * [evaluate] call that:
 *  1. Looks up the template by id.
 *  2. Runs [TemplateMatcher.evaluateBest] over the supplied candidates.
 *  3. Returns a typed [ImageTargetOutcome] with a highlighted region when matched.
 *
 * This is the "brain" layer for the image-target scenario type.
 * No Android imports, no bitmap I/O, no tap dispatch.
 * The real image-similarity provider (OpenCV / ML Kit) will supply the
 * [MatchCandidate] list in a later step; tests inject synthetic candidates.
 *
 * @param templateManager  Registry of known [CaptureTemplate]s.
 * @param matcher          Decision layer (injected for testability).
 */
class ImageTargetController(
    private val templateManager: TemplateManager,
    private val matcher: TemplateMatcher
) {

    /**
     * Evaluate the best candidate for [templateId] against the registered template.
     *
     * @param templateId  Id of the template to look up in [templateManager].
     * @param candidates  Raw candidates from the image-similarity provider.
     *                    May be empty (returns [ImageTargetOutcome.NoMatch]).
     * @return A typed [ImageTargetOutcome].
     */
    fun evaluate(
        templateId: String,
        candidates: List<MatchCandidate>
    ): ImageTargetOutcome {
        val template = templateManager.get(templateId)
            ?: return ImageTargetOutcome.Error(
                ImageTargetResult(
                    templateId = templateId,
                    matched = false,
                    confidence = 0f,
                    highlight = null,
                    evaluatedAtMs = matcher.nowMs(),
                    errorReason = "template_not_found"
                )
            )

        val matchResult = matcher.evaluateBest(template, candidates)

        val result = ImageTargetResult(
            templateId = matchResult.templateId,
            matched = matchResult.matched,
            confidence = matchResult.confidence,
            highlight = matchResult.highlight,
            evaluatedAtMs = matchResult.evaluatedAtMs
        )

        return if (matchResult.matched) {
            ImageTargetOutcome.Matched(result)
        } else {
            ImageTargetOutcome.NoMatch(result)
        }
    }
}
