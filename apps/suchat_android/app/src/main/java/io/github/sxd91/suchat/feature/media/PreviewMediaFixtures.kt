package io.github.sxd91.suchat.feature.media

import io.github.sxd91.suchat.feature.media.model.FileAsset
import io.github.sxd91.suchat.feature.media.model.MediaAsset

/** Permission-free placeholder content for the picker preview session. */
object PreviewMediaFixtures {
    val media = listOf(
        MediaAsset("preview-image-1", "preview://media/sunrise", "Sunrise.jpg", MediaType.Image, "image/jpeg", 2_431_008, 1_788_400_000_000, 3024, 4032, folderId = "camera"),
        MediaAsset("preview-image-2", "preview://media/city", "City lights.jpg", MediaType.Image, "image/jpeg", 1_982_445, 1_788_313_600_000, 3000, 2000, folderId = "camera"),
        MediaAsset("preview-image-3", "preview://media/sea", "Sea breeze.png", MediaType.Image, "image/png", 3_845_120, 1_788_227_200_000, 1920, 1080, folderId = "downloads"),
        MediaAsset("preview-video-1", "preview://media/walk", "Evening walk.mp4", MediaType.Video, "video/mp4", 18_220_032, 1_788_140_800_000, 1920, 1080, 24_000, "camera"),
    )

    val files = listOf(
        FileAsset("preview-file-1", "preview://file/trip", "Trip itinerary.pdf", "application/pdf", FileCategory.Document, 841_230, 1_788_400_000_000),
        FileAsset("preview-file-2", "preview://file/recording", "Voice memo.m4a", "audio/mp4", FileCategory.Audio, 4_220_810, 1_788_313_600_000),
        FileAsset("preview-file-3", "preview://file/photos", "Photos backup.zip", "application/zip", FileCategory.Archive, 12_830_200, 1_788_227_200_000),
    )
}
