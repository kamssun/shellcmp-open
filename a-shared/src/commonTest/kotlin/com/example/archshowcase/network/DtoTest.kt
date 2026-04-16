package com.example.archshowcase.network

import com.example.archshowcase.network.dto.ApiResponse
import com.example.archshowcase.network.dto.CreateUserRequest
import com.example.archshowcase.network.dto.ImageItem
import com.example.archshowcase.network.dto.ImagePageResponse
import com.example.archshowcase.network.dto.MemberInfoDto
import com.example.archshowcase.network.dto.UserDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DtoTest {

    @Test
    fun `UserDto properties`() {
        val user = UserDto(id = 1, name = "John", email = "john@test.com")
        assertEquals(1, user.id)
        assertEquals("John", user.name)
        assertEquals("john@test.com", user.email)
    }

    @Test
    fun `UserDto equality`() {
        assertEquals(
            UserDto(1, "A", "a@test.com"),
            UserDto(1, "A", "a@test.com")
        )
    }

    @Test
    fun `CreateUserRequest properties`() {
        val request = CreateUserRequest(name = "Alice", email = "alice@test.com")
        assertEquals("Alice", request.name)
        assertEquals("alice@test.com", request.email)
    }

    @Test
    fun `ApiResponse with data`() {
        val response = ApiResponse(code = 200, message = "OK", data = UserDto(1, "U", "u@t.com"))
        assertEquals(200, response.code)
        assertEquals("OK", response.message)
        assertEquals(1, response.data?.id)
    }

    @Test
    fun `ApiResponse with null data`() {
        val response = ApiResponse<UserDto>(code = 404, message = "Not found", data = null)
        assertNull(response.data)
    }

    @Test
    fun `ImageItem properties`() {
        val item = ImageItem(id = "img_1", url = "https://example.com/1.jpg", title = "Image 1")
        assertEquals("img_1", item.id)
        assertEquals("https://example.com/1.jpg", item.url)
        assertEquals("Image 1", item.title)
    }

    @Test
    fun `ImagePageResponse properties`() {
        val items = listOf(
            ImageItem("1", "url1", "T1"),
            ImageItem("2", "url2", "T2")
        )
        val response = ImagePageResponse(items = items, total = 100, hasMore = true)

        assertEquals(2, response.items.size)
        assertEquals(100, response.total)
        assertTrue(response.hasMore)
    }

    @Test
    fun `ImagePageResponse no more`() {
        val response = ImagePageResponse(items = emptyList(), total = 0, hasMore = false)
        assertFalse(response.hasMore)
        assertTrue(response.items.isEmpty())
    }

    @Test
    fun `MemberInfoDto with all nulls`() {
        val dto = MemberInfoDto()
        assertNull(dto.member_id)
        assertNull(dto.nickname)
        assertNull(dto.avatar_url)
    }

    @Test
    fun `MemberInfoDto with values`() {
        val dto = MemberInfoDto(
            member_id = "m1",
            nickname = "Nick",
            avatar_url = "https://example.com/avatar.jpg"
        )
        assertEquals("m1", dto.member_id)
        assertEquals("Nick", dto.nickname)
        assertEquals("https://example.com/avatar.jpg", dto.avatar_url)
    }
}
