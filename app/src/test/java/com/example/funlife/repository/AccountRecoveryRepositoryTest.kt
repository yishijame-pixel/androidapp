package com.example.funlife.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.funlife.account.CloudWalletSnapshot
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.User
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AccountRecoveryRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var coinRepository: CoinRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        userRepository = UserRepository(db.userDao())
        coinRepository = CoinRepository(db.coinDao(), ctx)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun recreateLocalAccount_insertsUserWhenMissing() = runBlocking {
        val user = userRepository.recreateLocalAccount("linkg", "Secret123!", "书童")
        assertThat(user.username).isEqualTo("linkg")
        assertThat(user.nickname).isEqualTo("书童")
        assertThat(user.id).isGreaterThan(0L)

        val loaded = userRepository.login("linkg", "Secret123!")
        assertThat(loaded).isNotNull()
        assertThat(loaded!!.id).isEqualTo(user.id)
    }

    @Test
    fun recreateLocalAccount_resetsPasswordWhenLocalRowExists() = runBlocking {
        val old = User(username = "linkg", password = "plain-wrong", nickname = "旧昵称")
        val id = db.userDao().insert(old)

        val rebuilt = userRepository.recreateLocalAccount("linkg", "NewPwd456!", "新昵称")
        assertThat(rebuilt.id).isEqualTo(id)
        assertThat(rebuilt.nickname).isEqualTo("新昵称")
        assertThat(userRepository.login("linkg", "NewPwd456!")).isNotNull()
        assertThat(userRepository.login("linkg", "plain-wrong")).isNull()
    }

    @Test
    fun restoreWalletFromCloudSnapshot_writesCoinsAndPoints() = runBlocking {
        val userId = db.userDao().insert(User(username = "u1", password = "x", nickname = "u1"))
        coinRepository.restoreWalletFromCloudSnapshot(
            userId,
            CloudWalletSnapshot(
                balance = 1200,
                totalEarned = 5000,
                totalSpent = 3800,
                pointsBalance = 42,
                hasSnapshot = true,
            ),
        )
        assertThat(coinRepository.getCoinsAmount(userId)).isEqualTo(1200)
        assertThat(coinRepository.getShopPoints(userId)).isEqualTo(42)
        assertThat(db.coinDao().getUserCoinsSync(userId)!!.totalEarned).isEqualTo(5000)
    }
}
