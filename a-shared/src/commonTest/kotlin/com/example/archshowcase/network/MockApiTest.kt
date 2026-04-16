package com.example.archshowcase.network

import com.example.archshowcase.network.dto.CreateUserRequest
import com.example.archshowcase.network.mock.MockImageApi
import com.example.archshowcase.network.mock.MockUserApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MockApiTest {

    // --- MockUserApi ---

    @Test
    fun `MockUserApi getUser returns user for valid id`() = runTest {
        val api = MockUserApi()
        val result = api.getUser("1")

        assertTrue(result.isSuccess)
        assertEquals("Mock User 1", result.getOrThrow().name)
        assertEquals("user1@mock.com", result.getOrThrow().email)
    }

    @Test
    fun `MockUserApi getUser returns failure for invalid id`() = runTest {
        val api = MockUserApi()
        val result = api.getUser("999")

        assertTrue(result.isFailure)
    }

    @Test
    fun `MockUserApi createUser creates with given data`() = runTest {
        val api = MockUserApi()
        val request = CreateUserRequest("New User", "new@test.com")
        val result = api.createUser(request)

        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("New User", user.name)
        assertEquals("new@test.com", user.email)
        assertTrue(user.id >= 100)
    }

    @Test
    fun `MockUserApi createUser generates incrementing ids`() = runTest {
        val api = MockUserApi()
        val r1 = api.createUser(CreateUserRequest("A", "a@test.com"))
        val r2 = api.createUser(CreateUserRequest("B", "b@test.com"))

        val id1 = r1.getOrThrow().id
        val id2 = r2.getOrThrow().id
        assertEquals(id1 + 1, id2)
    }

    @Test
    fun `MockUserApi getUsers returns all mock users`() = runTest {
        val api = MockUserApi()
        val result = api.getUsers()

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
    }

    // --- MockImageApi ---

    @Test
    fun `MockImageApi getImages returns first page`() = runTest {
        val api = MockImageApi()
        val result = api.getImages(offset = 0, limit = 20)

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals(20, response.items.size)
        assertEquals(100, response.total)
        assertTrue(response.hasMore)
    }

    @Test
    fun `MockImageApi getImages returns correct items`() = runTest {
        val api = MockImageApi()
        val response = api.getImages(offset = 0, limit = 5).getOrThrow()

        assertEquals("mock_0", response.items.first().id)
        assertEquals("Mock Image #1", response.items.first().title)
        assertEquals("mock_4", response.items.last().id)
    }

    @Test
    fun `MockImageApi getImages returns last page`() = runTest {
        val api = MockImageApi()
        val response = api.getImages(offset = 90, limit = 20).getOrThrow()

        assertEquals(10, response.items.size)
        assertFalse(response.hasMore)
    }

    @Test
    fun `MockImageApi getImages pagination`() = runTest {
        val api = MockImageApi()

        val page1 = api.getImages(offset = 0, limit = 20).getOrThrow()
        val page2 = api.getImages(offset = 20, limit = 20).getOrThrow()

        assertEquals(20, page1.items.size)
        assertEquals(20, page2.items.size)
        assertEquals("mock_0", page1.items.first().id)
        assertEquals("mock_20", page2.items.first().id)
    }

    @Test
    fun `MockImageApi getImages empty when offset exceeds total`() = runTest {
        val api = MockImageApi()
        val response = api.getImages(offset = 100, limit = 20).getOrThrow()

        assertTrue(response.items.isEmpty())
        assertFalse(response.hasMore)
    }
}
