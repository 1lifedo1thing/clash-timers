package com.kuswa.clashtimer.i18n

import android.content.Context
import com.kuswa.clashtimer.R
import com.kuswa.clashtimer.core.BoostType
import com.kuswa.clashtimer.core.Texts

/**
 * [Texts] backed by the app's resources, so the widget, the notifications and the tap log use the
 * language picked on the settings screen (or the system language when it is set to "跟随系统").
 */
fun Context.texts(): Texts {
    val res = AppLocale.wrap(this).resources
    return Texts(
        completed = res.getString(R.string.w_completed),
        idle = res.getString(R.string.w_idle),
        syncFailed = res.getString(R.string.w_sync_failed),
        noData = res.getString(R.string.w_no_data),
        laboratory = res.getString(R.string.w_laboratory),
        petHouse = res.getString(R.string.w_pet_house),
        craftedDefense = res.getString(R.string.w_crafted_defense),
        newSuffix = res.getString(R.string.w_new),
        unknownName = { id -> res.getString(R.string.w_unknown, id) },
        buildersLabel = { busy, total -> res.getString(R.string.w_builders, busy, total) },
        goblinTag = res.getString(R.string.w_goblin_tag),
        townHallTag = { level -> res.getString(R.string.w_town_hall_tag, level) },
        bite = res.getString(R.string.w_bite),
        defaultPlayerName = res.getString(R.string.chief),
        boostAdd = res.getString(R.string.w_boost_add),

        helpersBoth = { apprentice, assistant -> res.getString(R.string.w_helpers_both, apprentice, assistant) },
        apprenticeBusy = { job -> res.getString(R.string.w_apprentice_busy, job) },
        apprenticeIdle = res.getString(R.string.w_apprentice_idle),
        assistantBusy = { job -> res.getString(R.string.w_assistant_busy, job) },
        assistantIdle = res.getString(R.string.w_assistant_idle),
        helpersSeparator = res.getString(R.string.w_helpers_separator),
        syncedAgo = { ago -> res.getString(R.string.w_synced_ago, ago) },

        justNow = res.getString(R.string.w_just_now),
        minutesAgo = { n -> res.getString(R.string.w_minutes_ago, n) },
        hoursAgo = { n -> res.getString(R.string.w_hours_ago, n) },
        daysAgo = { n -> res.getString(R.string.w_days_ago, n) },
        daysHours = { d, h -> res.getString(R.string.w_days_hours, d, h) },
        hoursMinutes = { h, m -> res.getString(R.string.w_hours_minutes, h, m) },
        hoursOnly = { h -> res.getString(R.string.w_hours, h) },
        minutesOnly = { m -> res.getString(R.string.w_minutes, m) },

        syncFailedPrefix = { reason -> res.getString(R.string.sync_failed_prefix, reason) },
        clipboardEmpty = res.getString(R.string.reason_clipboard_empty),
        notVillageData = { reason -> res.getString(R.string.reason_not_village, reason) },
        exportTooOld = res.getString(R.string.reason_older),
        synced = res.getString(R.string.sync_synced),
        builderJobs = { n -> res.getQuantityString(R.plurals.sync_builder_jobs, n, n) },
        labPrefix = { name -> res.getString(R.string.sync_lab, name) },
        petPrefix = { name -> res.getString(R.string.sync_pet, name) },
        idleLower = res.getString(R.string.sync_idle),
        boostLeft = { left -> res.getString(R.string.sync_boost_left, left) },

        noticeBuilderFree = { name -> res.getString(R.string.notice_builder_free, name) },
        noticeLabFree = res.getString(R.string.notice_lab_free),
        noticePetFree = res.getString(R.string.notice_pet_free),
        noticeGoblinDone = { name -> res.getString(R.string.notice_goblin_done, name) },
        noticeBuilt = { name -> res.getString(R.string.notice_built, name) },
        noticeLevel = { name, level -> res.getString(R.string.notice_level, name, level) },

        apprenticeName = res.getString(R.string.helper_apprentice),
        assistantName = res.getString(R.string.helper_assistant),
        helperLinked = { helper, job -> res.getString(R.string.tap_helper_linked, helper, job) },
        helperMoved = { from -> res.getString(R.string.tap_helper_moved, from) },
        helperUnlinked = { helper, job -> res.getString(R.string.tap_helper_unlinked, helper, job) },
        helperNoChange = res.getString(R.string.tap_helper_no_change),
        boostAdded = { name -> res.getString(R.string.tap_boost_added, name) },
        boostIgnored = res.getString(R.string.tap_boost_ignored),
        boostUndo = { name -> res.getString(R.string.tap_boost_undo, name) },
        boostNothing = res.getString(R.string.tap_boost_nothing),
        unknownTap = { action, arg -> res.getString(R.string.tap_unknown, action, arg) },
        boostNames = mapOf(
            BoostType.BUILDER_POTION to res.getString(R.string.builder_potion),
            BoostType.BUILDER_BITE to res.getString(R.string.builder_bite),
            BoostType.RESEARCH_POTION to res.getString(R.string.research_potion),
            BoostType.PET_POTION to res.getString(R.string.pet_potion),
        ),
    )
}
