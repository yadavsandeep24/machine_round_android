package com.example.signing.internal

import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class OutboxItem(
    /** Generated once when signing; reused on every retry so the server can de-duplicate. */
    val requestId: String,
    val envelopeId: String,
    val signerName: String,
    val signedAtMillis: Long,
    val attempts: Int = 0,
    val lastError: String? = null,
    val rejected: Boolean = false
)

@Serializable
internal data class StoreData(
    val envelopes: List<EnvelopeDto> = emptyList(),
    val offlineDocs: Map<String, OfflineDocument> = emptyMap(),
    val outbox: List<OutboxItem> = emptyList()
)

/**
 * A single JSON file instead of Room: the SDK shouldn't force Room + KSP + a schema on every host app.
 * Writes are atomic (write temp file, then rename), so a crash mid-write never corrupts the outbox,
 * and disk is written BEFORE memory, so what the UI shows is always durable.
 */
internal class OfflineStore(
    private val file: File,
    private val io: CoroutineDispatcher,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val mutex = Mutex()
    private val _data = MutableStateFlow(load())
    val data: StateFlow<StoreData> = _data.asStateFlow()

    suspend fun update(transform: (StoreData) -> StoreData) = mutex.withLock {
        val next = transform(_data.value)
        withContext(io) { writeAtomically(json.encodeToString(StoreData.serializer(), next)) }
        _data.value = next
    }

    private fun load(): StoreData = if (!file.exists()) {
        StoreData()
    } else {
        // A corrupted file (e.g. from an older SDK version) must not crash the host app.
        runCatching { json.decodeFromString(StoreData.serializer(), file.readText()) }.getOrDefault(StoreData())
    }

    private fun writeAtomically(text: String) {
        val dir = checkNotNull(file.parentFile).apply { mkdirs() }
        val tmp = File(dir, "${file.name}.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.delete()
            check(tmp.renameTo(file)) { "Could not persist offline store" }
        }
    }
}
