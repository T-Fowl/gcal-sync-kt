package com.tfowl.gcal

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import com.google.api.client.util.DateTime as GoogleDateTime
import com.google.api.services.calendar.model.EventDateTime as GoogleEventDateTime

//////////////        Base translations         //////////////
////////////// Instant <--> Google translations //////////////

fun Instant.toGoogleDateTime(zone: ZoneId = ZoneId.systemDefault()): GoogleDateTime =
    GoogleDateTime(Date.from(this), TimeZone.getTimeZone(zone))

fun Instant.toGoogleEventDateTime(zone: ZoneId = ZoneId.systemDefault()): GoogleEventDateTime =
    GoogleEventDateTime().setDateTime(toGoogleDateTime(zone))

fun GoogleDateTime.toInstant(): Instant = Instant.ofEpochMilli(value)

fun GoogleEventDateTime.toInstant(): Instant = Instant.ofEpochMilli((dateTime ?: date).value)

////////////// LocalDate <--> Google translations //////////////

fun LocalDate.toGoogleDateTime(zone: ZoneId = ZoneId.systemDefault()): GoogleDateTime {
    val date = Date.from(atStartOfDay(zone).toInstant())
    val tz = TimeZone.getTimeZone(zone)
    // We have to do it this way to pass true for dateOnly, otherwise we could serialize then use the string constructor
    return GoogleDateTime(true, date.time, tz.getOffset(date.time) / 60_000)
}

fun LocalDate.toGoogleEventDateTime(zone: ZoneId = ZoneId.systemDefault()): GoogleEventDateTime =
    GoogleEventDateTime().setDate(toGoogleDateTime(zone))

fun GoogleDateTime.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    LocalDate.ofInstant(toInstant(), zone)

fun GoogleEventDateTime.toLocalDate(zone: ZoneId = ZoneId.systemDefault()) = LocalDate.ofInstant(toInstant(), zone)

////////////// LocalDateTime <--> Google translations //////////////

fun LocalDateTime.toGoogleDateTime(zone: ZoneId = ZoneId.systemDefault()): GoogleDateTime =
    atZone(zone).toInstant().toGoogleDateTime(zone)

fun LocalDateTime.toGoogleEventDateTime(zone: ZoneId = ZoneId.systemDefault()): GoogleEventDateTime =
    GoogleEventDateTime().setDateTime(toGoogleDateTime(zone))

fun GoogleDateTime.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    LocalDateTime.ofInstant(toInstant(), zone)

fun GoogleEventDateTime.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()) =
    LocalDateTime.ofInstant(toInstant(), zone)