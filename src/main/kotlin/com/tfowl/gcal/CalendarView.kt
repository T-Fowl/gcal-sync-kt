package com.tfowl.gcal

import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.services.calendar.model.Calendar
import com.google.api.services.calendar.Calendar as CalendarApi
import com.google.api.services.calendar.model.Event

fun CalendarApi.calendarView(id: String) = CalendarView(this, id)

class CalendarView(
    private val api: CalendarApi,
    private val calendarId: String,
) {
    val details: Calendar by lazy { api.calendars().get(calendarId).execute() }

    fun batch(): BatchRequest = api.batch()
    fun list() = api.events().list(calendarId)
    fun insert(event: Event) = api.events().insert(calendarId, event)
    fun update(id: String, content: Event) = api.events().update(calendarId, id, content)
    fun delete(id: String) = api.events().delete(calendarId, id)
    fun get(id: String) = api.events().get(calendarId, id)
    fun patch(id: String, content: Event) = api.events().patch(calendarId, id, content)
}