package com.example.funlife.vip

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AccountRecoveryLogicTest {

    @Test
    fun passwordProof_isDeterministic() {
        val a = UserCloudRepository.computePasswordProof("linkg", "Secret123!")
        val b = UserCloudRepository.computePasswordProof("linkg", "Secret123!")
        assertThat(a).isEqualTo(b)
        assertThat(a).hasLength(64)
    }

    @Test
    fun passwordProof_changesWithPassword() {
        val a = UserCloudRepository.computePasswordProof("linkg", "Secret123!")
        val b = UserCloudRepository.computePasswordProof("linkg", "OtherPwd!")
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun passwordProof_trimsUsernameInFormula() {
        val a = UserCloudRepository.computePasswordProof("linkg", "pwd")
        val b = UserCloudRepository.computePasswordProof("  linkg  ", "pwd")
        assertThat(a).isEqualTo(b)
    }
}
