# Consumer R8 rules shipped with the SDK (applied when the host app minifies).
# Public API classes are kept by normal reachability from host code.
# SigningActivity is referenced from the merged manifest, so it's kept automatically.
# kotlinx.serialization ships its own rules for @Serializable classes.
