package io.github.sxd91.suchat.feature.media.model

import io.github.sxd91.suchat.feature.media.FileCategory

/** A user-authorized document or application-managed file. */
data class FileAsset(
    val id: String,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val category: FileCategory,
    val sizeBytes: Long,
    val modifiedAtEpochMillis: Long,
)
