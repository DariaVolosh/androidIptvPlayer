package com.example.iptvplayer.integrationTests.data

import android.content.Context
import com.example.iptvplayer.data.repositories.AuthRepository
import com.example.iptvplayer.domain.auth.AuthManager
import com.example.iptvplayer.retrofit.data.AuthInfo
import com.example.iptvplayer.retrofit.data.ChannelsAndEpgAuthResponse
import com.example.iptvplayer.retrofit.services.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.parse
import okhttp3.RequestBody
import okhttp3.RequestBody.create
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AuthManagerAndAuthRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var context: Context
    @Mock private lateinit var authService: AuthService

    private lateinit var authManager: AuthManager
    private lateinit var authRepository: AuthRepository

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        authRepository = AuthRepository(
            context = context,
            authService = authService
        )

        authManager = AuthManager(
            authRepository = authRepository
        )
    }

    @Test
    fun getAuthToken_tokenIsAvailable_returnsToken() = runTest {
        val mockToken = "mockToken"
        val mockAuthResponse = ChannelsAndEpgAuthResponse(
            data = AuthInfo(
                token = mockToken
            )
        )
        whenever(authService.getAuthToken(any<RequestBody>())).thenReturn(mockAuthResponse)

        val dummyRequestBody = create(parse("application/json"), "{}")
        val dummyErrorCallback: (String, String) -> Unit = { title, description -> }
        val returnedToken = authManager.getAuthToken(dummyRequestBody, dummyErrorCallback)

        assertEquals(mockToken, returnedToken)
    }

    @Test
    fun getAuthToken_tokenIsNotAvailable_returnsEmptyTokenAndExecutesAuthErrorCallback() = runTest {
        whenever(authService.getAuthToken(any<RequestBody>())).thenReturn(
            ChannelsAndEpgAuthResponse(null)
        )
        whenever(context.getString(any())).thenAnswer { "" }

        val dummyRequestBody = create(parse("application/json"), "{}")
        val mockAuthErrorCallback: Function2<String, String, Unit> = mock()
        val returnedToken = authManager.getAuthToken(dummyRequestBody, mockAuthErrorCallback)

        assertEquals("", returnedToken)

        verify(mockAuthErrorCallback, times(1)).invoke(any(), any())
    }
}