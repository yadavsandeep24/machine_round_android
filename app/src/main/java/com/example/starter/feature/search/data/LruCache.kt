package com.example.starter.feature.search.data

/**
 * Minimal LRU cache: LinkedHashMap with access order evicts the least recently used entry.
 * Interview talking point: android.util.LruCache does the same but needs the Android runtime in unit tests.
 */
class LruCache<K, V>(private val maxSize: Int) {
    init {
        require(maxSize > 0) { "maxSize must be > 0" }
    }

    private val map = object : LinkedHashMap<K, V>(maxSize, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxSize
    }

    @Synchronized operator fun get(key: K): V? = map[key]

    @Synchronized operator fun set(key: K, value: V) {
        map[key] = value
    }

    @Synchronized fun size(): Int = map.size

    private companion object {
        const val LOAD_FACTOR = 0.75f
    }
}
