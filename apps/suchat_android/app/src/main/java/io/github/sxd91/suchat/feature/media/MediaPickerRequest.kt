package io.github.sxd91.suchat.feature.media

/** Configuration supplied by a media-picker caller. */
data class MediaPickerRequest(
    val purpose: MediaPickerPurpose,
    val allowedTypes: Set<MediaType> = setOf(MediaType.Image),
    val selectionLimit: Int = purpose.defaultSelectionLimit,
    val allowOriginalQuality: Boolean = false,
) {
    init {
        require(selectionLimit > 0) { "selectionLimit must be greater than zero" }
        require(allowedTypes.isNotEmpty()) { "allowedTypes must not be empty" }
    }
}

enum class MediaPickerPurpose(val defaultSelectionLimit: Int) {
    Chat(9),
    Moments(9),
    Avatar(1),
    ThemeWallpaper(1),
    DriftBottle(3),
    Import(9),
}

enum class MediaType { Image, Video }
