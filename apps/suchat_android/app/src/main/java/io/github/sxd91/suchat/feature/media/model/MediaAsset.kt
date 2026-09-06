package io.github.sxd91.suchat.feature.media.model

import io.github.sxd91.suchat.feature.media.MediaType

/** A media item known to the embedded picker. */
data class MediaAsset(
    val id: String,
    val uri: String,
    val displayName: String,
    val mediaType: MediaType,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedAtEpochMillis: Long,
    val width: Int? = null,
    val height: Int? = null,
    val durationMillis: Long? = null,
    val folderId: String? = null,
)
