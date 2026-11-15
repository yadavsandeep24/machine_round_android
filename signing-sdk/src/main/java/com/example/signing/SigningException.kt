package com.example.signing

/** Every failure the SDK surfaces is one of these, so hosts can handle them exhaustively. */
public sealed class SigningException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    public class Network(cause: Throwable) : SigningException("Network unavailable", cause)

    /** The host's [TokenProvider] returned a token the server rejected, even after one refresh. */
    public class Unauthorized : SigningException("Access token rejected")
    public class EnvelopeNotFound(public val envelopeId: String) : SigningException("Envelope $envelopeId not found")
    public class NotAvailableOffline(public val envelopeId: String) :
        SigningException("Envelope $envelopeId is not stored for offline signing")
}
