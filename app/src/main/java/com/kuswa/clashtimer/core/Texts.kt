package com.kuswa.clashtimer.core

/**
 * Every user-visible string the widget model, the sync summary, the notifications and the tap log
 * need. The core stays free of `android.*` so it can be unit tested; the defaults below are English
 * and are what the tests see. [com.kuswa.clashtimer.i18n.AndroidTexts] builds the same object from
 * the app's resources, so the widget and the notifications follow the in-app language choice.
 *
 * Text that only Compose screens show lives in `res/values/strings.xml` and is read with
 * `stringResource()`; this file covers what is produced outside a composable.
 */
data class Texts(
    // ---- widget rows ----
    val completed: String = "Completed",
    val idle: String = "Idle",
    val syncFailed: String = "Sync failed — copy data and tap Sync",
    val noData: String = "Copy village data in Clash, then tap Sync",
    val laboratory: String = "Laboratory",
    val petHouse: String = "Pet House",
    val craftedDefense: String = "Crafted defense",
    val newSuffix: String = "(new)",
    val unknownName: (Int) -> String = { "Unknown ($it)" },
    val buildersLabel: (Int, Int) -> String = { busy, total -> "BUILDERS $busy/$total" },
    val goblinTag: String = " + Goblin",
    val townHallTag: (Int) -> String = { " TH$it" },
    val bite: String = "Bite",
    /** Header name when the player never set one: `Chief` / `首领`. */
    val defaultPlayerName: String = "Chief",
    val boostAdd: String = "+1",

    // ---- footer ----
    val helpersBoth: (String, String) -> String = { a, b -> "Helpers: $a · $b" },
    val apprenticeBusy: (String) -> String = { "Apprentice: $it" },
    val apprenticeIdle: String = "Apprentice idle",
    val assistantBusy: (String) -> String = { "Assistant: $it" },
    val assistantIdle: String = "Assistant idle",
    val helpersSeparator: String = " · ",
    val syncedAgo: (String) -> String = { "synced $it" },

    // ---- durations ----
    val justNow: String = "just now",
    val minutesAgo: (Long) -> String = { "${it}m ago" },
    val hoursAgo: (Long) -> String = { "${it}h ago" },
    val daysAgo: (Long) -> String = { "${it}d ago" },
    val daysHours: (Long, Long) -> String = { d, h -> "${d}d ${h}h" },
    val hoursMinutes: (Long, Long) -> String = { h, m -> "${h}h ${m}m" },
    val hoursOnly: (Long) -> String = { "${it}h" },
    val minutesOnly: (Long) -> String = { "${it}m" },

    // ---- sync summary ----
    val syncFailedPrefix: (String) -> String = { "Sync failed: $it" },
    val clipboardEmpty: String = "Clipboard is empty",
    val notVillageData: (String) -> String = { "Not village data: $it" },
    val exportTooOld: String = "Clipboard export is older than the last sync",
    val synced: String = "Synced",
    val builderJobs: (Int) -> String = { n -> " · $n builder job" + if (n == 1) "" else "s" },
    val labPrefix: (String) -> String = { " · lab: $it" },
    val petPrefix: (String) -> String = { " · pet: $it" },
    val idleLower: String = "idle",
    val boostLeft: (String) -> String = { " · builder potion $it left" },

    // ---- finish notifications ----
    val noticeBuilderFree: (String) -> String = { "Builder free: $it" },
    val noticeLabFree: String = "Lab free",
    val noticePetFree: String = "Pet house free",
    val noticeGoblinDone: (String) -> String = { "Goblin Builder done: $it" },
    val noticeBuilt: (String) -> String = { "$it is built." },
    val noticeLevel: (String, Int) -> String = { name, level -> "$name reached level $level." },

    // ---- widget tap log ----
    val apprenticeName: String = "Apprentice",
    val assistantName: String = "Lab Assistant",
    val helperLinked: (String, String) -> String = { helper, job -> "$helper linked to $job" },
    val helperMoved: (String) -> String = { " (moved from $it)" },
    val helperUnlinked: (String, String) -> String = { helper, job -> "$helper unlinked from $job" },
    val helperNoChange: String = "Helper tap changed nothing",
    val boostAdded: (String) -> String = { "$it +1 h" },
    val boostIgnored: String = " (ignored)",
    val boostUndo: (String) -> String = { "$it undo" },
    val boostNothing: String = " (nothing to undo)",
    val unknownTap: (String, String) -> String = { action, arg -> "Unknown tap $action/$arg" },
    val boostNames: Map<BoostType, String> = mapOf(
        BoostType.BUILDER_POTION to "Builder potion",
        BoostType.BUILDER_BITE to "Builder Bite",
        BoostType.RESEARCH_POTION to "Research potion",
        BoostType.PET_POTION to "Pet potion",
    ),
) {
    fun boostName(type: BoostType): String = boostNames[type] ?: type.name
}
