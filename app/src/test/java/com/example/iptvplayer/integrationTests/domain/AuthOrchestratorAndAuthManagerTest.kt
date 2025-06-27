package com.example.iptvplayer.integrationTests.domain

import app.cash.turbine.turbineScope
import com.example.iptvplayer.data.repositories.AuthRepository
import com.example.iptvplayer.domain.auth.AuthManager
import com.example.iptvplayer.domain.auth.AuthOrchestrator
import com.example.iptvplayer.domain.auth.AuthState
import com.example.iptvplayer.domain.errors.ErrorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AuthOrchestratorAndAuthManagerTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var authRepository: AuthRepository
    @Mock private lateinit var errorManager: ErrorManager
    private lateinit var authOrchestrator: AuthOrchestrator
    private lateinit var authManager: AuthManager

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }


    @Test
    fun getBackendToken_tokenAvailable_authStateChangesToAuthenticatedAndTokenUpdated() = runTest {
        turbineScope {
            val mockToken = "mockToken"
            whenever(authRepository.getAuthToken(any(), any())).thenReturn(mockToken)

            authManager = AuthManager(
                authRepository = authRepository
            )

            authOrchestrator = AuthOrchestrator(
                errorManager = errorManager,
                authManager = authManager,
                orchestratorScope = backgroundScope
            )

            val authStateCollector = authOrchestrator.authState.testIn(this)
            val tokenCollector = authManager.token.testIn(this)

            assertEquals("", tokenCollector.awaitItem())
            assertEquals(AuthState.AUTHENTICATING, authStateCollector.awaitItem())

            authOrchestrator.getBackendToken()

            assertEquals(mockToken, tokenCollector.awaitItem())
            assertEquals(AuthState.AUTHENTICATED, authStateCollector.awaitItem())

            authStateCollector.cancelAndIgnoreRemainingEvents()
            tokenCollector.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getBackendToken_tokenIsNotAvailable_authStateChangesToErrorAndTokenIsNotUpdated() = runTest {
        turbineScope {
            val mockToken = ""
            whenever(authRepository.getAuthToken(any(), any())).thenReturn(mockToken)

            authManager = AuthManager(
                authRepository = authRepository
            )

            authOrchestrator = AuthOrchestrator(
                errorManager = errorManager,
                authManager = authManager,
                orchestratorScope = backgroundScope
            )

            val authStateCollector = authOrchestrator.authState.testIn(this)
            val tokenCollector = authManager.token.testIn(this)

            assertEquals("", tokenCollector.awaitItem())
            assertEquals(AuthState.AUTHENTICATING, authStateCollector.awaitItem())

            authOrchestrator.getBackendToken()

            tokenCollector.expectNoEvents()
            assertEquals(AuthState.ERROR, authStateCollector.awaitItem())

            authStateCollector.cancelAndIgnoreRemainingEvents()
            tokenCollector.cancelAndIgnoreRemainingEvents()
        }
    }
}