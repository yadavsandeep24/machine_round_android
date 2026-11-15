package com.example.starter.feature.search.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LruCacheTest {
    @Test
    fun `evicts least recently used, and reads count as use`() {
        val cache = LruCache<String, Int>(2)
        cache["a"] = 1
        cache["b"] = 2
        cache["a"] // touch a -> b is now eldest
        cache["c"] = 3
        assertThat(cache["b"]).isNull()
        assertThat(cache["a"]).isEqualTo(1)
        assertThat(cache["c"]).isEqualTo(3)
        assertThat(cache.size()).isEqualTo(2)
    }
}
