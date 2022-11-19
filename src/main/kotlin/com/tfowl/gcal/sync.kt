package com.tfowl.gcal

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import java.time.LocalDate
import java.time.ZoneId

/* TODO: Store and use the following properties:
*   [x] Summary               - perform 3 way merge when updating (old, manual, new)
*   [ ] Description           - perform 3 way merge when updating (old, manual, new)
*   [ ] Start & End datetimes - keep a reference to any manual edits
*
* */
private const val EXT_PROP_KEY_SUMMARY = "summary"

private fun modifySummary(
    originalGenerated: String,
    manuallyModified: String,
    newGenerated: String,
): String {

    // If we generated the same summary which had been changed anyway
    if (newGenerated.equals(originalGenerated, ignoreCase = true)) return manuallyModified

    // If the original summary was appended or prepended to
    if (manuallyModified.contains(originalGenerated, ignoreCase = true)) {
        val originalStart = manuallyModified.indexOf(originalGenerated, ignoreCase = true)

        return manuallyModified.replaceRange(originalStart, originalStart + originalGenerated.length, newGenerated)
    }

    // Cannot determine how to set the new title without overwriting manual edits
    // Simply just concatenate the two

    return "$newGenerated / $manuallyModified"
}

private fun <T, K> Iterable<T>.associateByNotNull(k: (T) -> K?): Map<K, T> {
    val destination = LinkedHashMap<K, T>()
    for (item in this) {
        k(item)?.let { key -> destination.put(key, item) }
    }
    return destination
}

fun sync(
    service: Calendar,
    calendar: CalendarView,
    range: ClosedRange<LocalDate>,
    targetEvents: List<Event>,
    zone: ZoneId,
    domain: String,
) {
    targetEvents.forEach { e ->
        // Store a reference to the generated summary so we can maintain manual edits
        e.extendedProperties.private[EXT_PROP_KEY_SUMMARY] = e.summary
    }


    val currentEvents = calendar.list()
        .setTimeMin(range.start.atStartOfDay(zone).toInstant().toGoogleDateTime(zone))
        .setTimeMax(range.endInclusive.plusDays(1).atStartOfDay(zone).toInstant().toGoogleDateTime(zone))
        .setShowDeleted(true)
        .execute().items
        .filter { it.iCalUID?.endsWith("@$domain") == true }

    val (create, update, delete) = computeSyncActions(calendar, currentEvents, targetEvents)

    println("Creating ${create.size} events...")
    println("Updating ${update.size} events...")
    println("Deleting ${delete.size} events...")

    val batch = service.batch()

    for (event in delete) {
        calendar.delete(event.id).queue(batch) { _, res ->
            when (res) {
                is Ok  -> println("Successfully deleted event ${event.pretty()}")
                is Err -> println("Failed to delete event ${event.pretty()}: ${res.error}")
            }
        }
    }

    for ((id, event) in update) {
        calendar.update(id, event).queue(batch) { _, res ->
            when (res) {
                is Ok  -> println("Successfully updated event ${event.pretty()}")
                is Err -> println("Failed to update event ${event.pretty()}: ${res.error}")
            }
        }
    }

    // Create can be a special case if for some reason an event with the same id already exists outside the sync period

    for (event in create) {
        fun updateTheExistingEvent(id: String) {
            calendar.update(id, event).queue(batch) { _, res ->
                when (res) {
                    is Ok  -> println("Successfully updated the existing event for ${event.pretty()}")
                    is Err -> println("Failed to update the existing event for ${event.pretty()}: ${res.error}")
                }
            }
        }

        fun findTheExistingEvent() {
            calendar.list().setICalUID(event.iCalUID).setShowDeleted(true).queue(batch) { _, res ->
                when (res) {
                    is Ok  -> updateTheExistingEvent(res.value.items.single().id)
                    is Err -> println("Failed to find the existing event for ${event.pretty()}: ${res.error}")
                }
            }
        }

        fun createNewEvent() {
            calendar.insert(event).queue(batch) { _, res ->
                when (res) {
                    is Ok  -> println("Successfully created event: ${event.pretty()}")
                    is Err -> {
                        if (res.error.code == 409) {
                            println("Failed to create event ${event.pretty()} because it already exists - will try and update the existing event")
                            findTheExistingEvent()
                        } else {
                            println("Failed to create event ${event.pretty()}: ${res.error}")
                        }
                    }
                }
            }
        }

        createNewEvent()
    }

    batch.execute() // Execute all deletes, updates and creates
    if (batch.size() > 0) batch.execute() // Execute all lists - finding the existing events when creates fail
    if (batch.size() > 0) batch.execute() // Execute all updated to the found existing events when creates fail
}

private data class SyncActions(val create: List<Event>, val update: Map<String, Event>, val delete: List<Event>)

private fun computeSyncActions(
    calendarView: CalendarView,
    existingEvents: List<Event>,
    requiredEvents: List<Event>,
): SyncActions {
    fun List<Event>.noneHaveICalId(id: String): Boolean = none { it.iCalUID == id }
    fun List<Event>.findWithICalId(id: String): Event? = find { it.iCalUID == id }

    val create = requiredEvents.filter { required ->
        existingEvents.noneHaveICalId(required.iCalUID)
    }

    val update = requiredEvents.associateByNotNull { required ->
        val existing = existingEvents.findWithICalId(required.iCalUID) ?: return@associateByNotNull null

        if (null != existing.extendedProperties?.private) {
            if (EXT_PROP_KEY_SUMMARY in existing.extendedProperties.private) {
                val oldSummary = existing.extendedProperties.private[EXT_PROP_KEY_SUMMARY]!!

                // User has manually changed the summary
                if (!existing.summary.equals(oldSummary)) {
                    // We mutate the event to be some diff between the old summary, the user modified one, and the new one
                    required.setSummary(
                        modifySummary(
                            oldSummary,
                            existing.summary,
                            required.summary
                        )
                    )
                }
            }
        }

        existing.id
    }

    val delete = existingEvents.filter { existing ->
        requiredEvents.noneHaveICalId(existing.iCalUID)
    }.filter { !it.isCancelled() } // Cannot delete something which is already deleted

    return SyncActions(create, update, delete)
}