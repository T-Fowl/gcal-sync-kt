package com.tfowl.gcal

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest
import com.google.api.client.http.HttpHeaders

fun <T, R : AbstractGoogleJsonClientRequest<T>> R.queue(
    batch: BatchRequest,
    callback: (R, Result<T, GoogleJsonError>) -> Unit = { _, _ -> Unit },
) {
    queue(batch, object : JsonBatchCallback<T>() {
        @Suppress("UNCHECKED_CAST")
        val req = this@queue.clone() as R

        override fun onSuccess(t: T, responseHeaders: HttpHeaders?) = callback(req, Ok(t))
        override fun onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders?) = callback(req, Err(e))
    })
}