package com.kuswa.clashtimer.core

/** What happens to the stored state when the player taps Sync. */
object SyncLogic {

    sealed interface Outcome {
        val state: AppState

        data class Synced(override val state: AppState, val export: VillageExport) : Outcome
        data class Failed(override val state: AppState, val reason: String) : Outcome
    }

    fun apply(old: AppState, clipboard: String?, texts: Texts = Texts()): Outcome {
        fun fail(reason: String) = Outcome.Failed(old.copy(syncFailed = true), reason)

        if (clipboard.isNullOrBlank()) return fail(texts.clipboardEmpty)
        val export = when (val r = ExportParser.parse(clipboard)) {
            is ParseResult.Invalid -> return fail(texts.notVillageData(r.reason))
            is ParseResult.Success -> r.export
        }
        val previous = old.export
        if (previous != null && export.timestamp < previous.timestamp) {
            return fail(texts.exportTooOld)
        }

        val links = HelperLinks(
            apprenticeJobKey = linkFor(JobCategory.BUILDER, export, previous, old.links.apprenticeJobKey),
            labAssistantJobKey = linkFor(JobCategory.LAB, export, previous, old.links.labAssistantJobKey),
        )
        var boosts = old.boosts.filter { it.end > export.timestamp }
        // `boosts.builder_boost` is the game's own remaining builder-potion time (stacked potions included),
        // so it replaces any builder-potion windows tapped on the widget instead of adding to them.
        export.builderBoostRemaining?.let { left ->
            boosts = boosts.filterNot { it.type == BoostType.BUILDER_POTION } +
                BoostUse(BoostType.BUILDER_POTION, export.timestamp, export.timestamp + left)
        }

        return Outcome.Synced(
            AppState(
                exportJson = clipboard.trim(),
                links = links,
                boosts = boosts,
                syncFailed = false,
                firstRemaining = export.jobs.associate { job ->
                    // max(): a key can be reused by a different instance (e.g. the second of two
                    // identical upgrades after the first finished); never shrink the bar's length.
                    job.key to maxOf(old.firstRemaining[job.key] ?: 0L, job.remainingAtT0)
                },
            ),
            export,
        )
    }

    /**
     * `helper_recurrent: true` on a running job sets the link. On a finished item (no `timer`) the helper
     * is idle → no link. Otherwise a manual link from the widget is carried over if its job still runs.
     */
    private fun linkFor(category: JobCategory, export: VillageExport, previous: VillageExport?, oldKey: String?): String? {
        val candidates = export.jobs.filter { it.category == category }
        candidates.firstOrNull { it.helperLinked }?.let { return it.key }
        if (category in export.helperOnFinishedItem) return null
        return carryOver(oldKey, previous, candidates)
    }

    /**
     * Keeps a manual link only if the same job (category + data + target level) still exists.
     * Prefers the exact same occurrence (e.g. the second of two identical Wizard Towers).
     */
    private fun carryOver(key: String?, previous: VillageExport?, candidates: List<Job>): String? {
        if (key == null) return null
        candidates.firstOrNull { it.key == key }?.let { return it.key }
        val old = previous?.jobs?.firstOrNull { it.key == key } ?: return null
        return candidates.firstOrNull {
            it.category == old.category && it.dataId == old.dataId && it.targetLevel == old.targetLevel
        }?.key
    }
}

/** Plain-text summary of a sync outcome for the toast and the launcher screen. */
fun SyncLogic.Outcome.describe(names: NameMap, texts: Texts = Texts()): String = when (this) {
    is SyncLogic.Outcome.Failed -> texts.syncFailedPrefix(reason)
    is SyncLogic.Outcome.Synced -> buildString {
        val e = export
        append(texts.synced)
        e.townHallLevel?.let { append(texts.townHallTag(it).trim().let { tag -> " $tag" }) }
        val goblin = e.builderJobs.count { it.isGoblin }
        append(texts.builderJobs(e.regularBuilderJobs.size))
        if (goblin > 0) append(texts.goblinTag.trim().let { " $it" })
        append(texts.labPrefix(e.labJob?.let(names::label) ?: texts.idleLower))
        append(texts.petPrefix(e.petJob?.let(names::label) ?: texts.idleLower))
        e.builderBoostRemaining?.let {
            append(texts.boostLeft(WidgetModelBuilder.formatRemaining(it, texts)))
        }
    }
}
