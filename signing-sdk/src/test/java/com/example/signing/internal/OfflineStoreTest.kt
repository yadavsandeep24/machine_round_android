package com.example.signing.internal

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfflineStoreTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun file() = File(tmp.root, "sdk/offline.json")

    @Test
    fun `writes survive a restart and leave no temp file behind`() = runTest {
        OfflineStore(file(), Dispatchers.Unconfined).update { it.copy(outbox = listOf(OutboxItem("r1", "env-1", "Asha", 1))) }
        val reloaded = OfflineStore(file(), Dispatchers.Unconfined)
        assertThat(reloaded.data.value.outbox.single().requestId).isEqualTo("r1")
        assertThat(File(tmp.root, "sdk/offline.json.tmp").exists()).isFalse()
    }

    @Test
    fun `a corrupted file starts empty instead of crashing the host app`() {
        file().apply {
            parentFile!!.mkdirs()
            writeText("{not json")
        }
        assertThat(OfflineStore(file(), Dispatchers.Unconfined).data.value).isEqualTo(StoreData())
    }
}
