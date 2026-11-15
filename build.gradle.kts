plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// Public API of the SDK is tracked in signing-sdk/api/signing-sdk.api.
// `./gradlew apiCheck` fails the build on any unintended public API change; `apiDump` updates it.
apiValidation {
    ignoredProjects += listOf("app")
}
