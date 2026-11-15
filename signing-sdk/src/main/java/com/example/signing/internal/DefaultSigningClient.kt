package com.example.signing.internal

import com.example.signing.Envelope
import com.example.signing.EnvelopeStatus
import com.example.signing.Recipient
import com.example.signing.SigningClient
import com.example.signing.SigningEventListener
import com.example.signing.SigningException
import com.example.signing.SigningResult
import com.example.signing.SigningSession
import com.example.signing.SyncReport
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class DefaultSigningClient(
    private val service: EnvelopeService,
    private val store: OfflineStore,
    private val tokens: TokenManager,
    events: SigningEventListener,
    private val clock: () -> Long,
    private val newRequestId: () -> String = { UUID.randomUUID().toString() }
) : SigningClient {

    private val syncer = OfflineSyncer(store, service, tokens, events)

    override val envelopes: Flow<List<Envelope>> = store.data.map(::merge).distinctUntilChanged()

    override suspend fun refresh() = mapErrors {
        val remote = tokens.withToken { service.listEnvelopes(it) }
        store.update { it.copy(envelopes = remote) }
    }

    override suspend fun makeAvailableOffline(envelopeId: String) = mapErrors {
        val doc = tokens.withToken { service.fetchDocument(it, envelopeId) }
        store.update { it.copy(offlineDocs = it.offlineDocs + (envelopeId to doc)) }
    }

    override suspend fun signOffline(envelopeId: String, signerName: String) {
        require(signerName.isNotBlank()) { "signerName must not be blank" }
        val data = store.data.value
        if (envelopeId !in data.offlineDocs) throw SigningException.NotAvailableOffline(envelopeId)
        if (data.outbox.any { it.envelopeId == envelopeId }) return // already signed: keep the first signature
        store.update {
            it.copy(outbox = it.outbox + OutboxItem(newRequestId(), envelopeId, signerName.trim(), clock()))
        }
    }

    override suspend fun syncPending(): SyncReport = mapErrors { syncer.sync() }

    override suspend fun createSigningSession(envelopeId: String, recipient: Recipient): SigningSession = mapErrors {
        val url = tokens.withToken { service.createSigningSession(it, envelopeId, recipient.email) }
        SigningSession(envelopeId, url)
    }

    override suspend fun handleResult(result: SigningResult) {
        if (result is SigningResult.Completed) {
            mapErrors { tokens.withToken { service.confirmOnlineCompletion(it, result.envelopeId) } }
            refresh()
        }
    }

    private fun merge(data: StoreData): List<Envelope> = data.envelopes.map { dto ->
        val pending = data.outbox.firstOrNull { it.envelopeId == dto.id }
        val status = when {
            dto.status == InMemoryEnvelopeService.STATUS_VOIDED || pending?.rejected == true -> EnvelopeStatus.VOIDED
            dto.status == InMemoryEnvelopeService.STATUS_COMPLETED -> EnvelopeStatus.COMPLETED
            pending != null -> EnvelopeStatus.SIGNED_PENDING_SYNC
            else -> EnvelopeStatus.SENT
        }
        Envelope(dto.id, dto.title, dto.sender, status, availableOffline = dto.id in data.offlineDocs)
    }

    /** Internal exception types never leak into the public API. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun <T> mapErrors(block: suspend () -> T): T = try {
        block()
    } catch (e: IOException) {
        throw SigningException.Network(e)
    } catch (e: UnauthorizedException) {
        throw SigningException.Unauthorized()
    } catch (e: EnvelopeNotFoundException) {
        throw SigningException.EnvelopeNotFound(e.envelopeId)
    }
}
