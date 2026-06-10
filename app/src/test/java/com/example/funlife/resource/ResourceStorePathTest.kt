package com.example.funlife.resource

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ResourceStorePathTest {

    @Test
    fun cloudRoots_detected() {
        assertThat(ResourceStore.isCloudResource("pet/cat/a.png")).isTrue()
        assertThat(ResourceStore.isCloudResource("login/a.png")).isTrue()
        assertThat(ResourceStore.isCloudResource("pac_maze/levels/1.json")).isFalse()
    }
}
