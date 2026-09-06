package io.github.sxd91.suchat.data.remote

data class ApiError(val statusCode: Int, val code: String?, val body: String) : Exception("HTTP $statusCode" + (code?.let { ": $it" } ?: ""))
data class HealthStatus(val status: String, val service: String, val version: String, val time: String, val database: String)
data class AuthRequest(val handle: String, val password: String)
data class RegisterRequest(val handle: String, val displayName: String, val password: String)
data class AuthTokens(val accessToken: String)
data class UserProfile(val id: String, val handle: String, val displayName: String, val avatarUrl: String?, val bio: String?, val status: String, val createdAt: String?, val lastSeenAt: String?)
data class AuthResponse(val user: UserProfile, val tokens: AuthTokens)
data class Appearance(val glassMode: String = "liquid_glass", val performanceProfile: String = "full", val pageTransition: String = "shared_element", val themeMode: String = "system", val reducedMotion: Boolean = false)
data class Conversation(val id: String, val kind: String, val title: String?, val createdAt: String?, val updatedAt: String?, val lastMessage: String?)
data class CreateConversationRequest(val memberIds: List<String>, val kind: String = "direct", val title: String? = null)
data class Message(val id: String, val conversationId: String, val senderId: String, val content: String, val contentType: String, val attachmentId: String?, val createdAt: String, val editedAt: String?)
data class SendMessageRequest(val content: String = "", val contentType: String = "text", val attachmentId: String? = null)
