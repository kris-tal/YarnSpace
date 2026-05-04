package com.yarnspace.app.data.user
import com.yarnspace.app.domain.feed.UserSummary
import com.yarnspace.app.retrofit.ApiService

class RemoteUserRepository(private val apiService: ApiService) : UserRepository {
    override suspend fun searchUsers(query: String): List<UserSummary> {
        if (query.isBlank()) return emptyList()
        return try {
            val remoteUsers = apiService.searchUsers(query)
            remoteUsers.map { dto ->
                UserSummary(
                    id = dto.id.toLong(),
                    username = dto.username,
                    displayName = dto.displayName,
                    avatarUrl = dto.avatarUrl,
                    accentColor = dto.accentColor
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
