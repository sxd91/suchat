package io.github.sxd91.suchat.feature.media

/** Configuration supplied by a file-picker caller. */
data class FilePickerRequest(
    val purpose: FilePickerPurpose = FilePickerPurpose.ChatAttachment,
    val allowedCategories: Set<FileCategory> = FileCategory.entries.toSet(),
    val selectionLimit: Int = purpose.defaultSelectionLimit,
) {
    init {
        require(selectionLimit > 0) { "selectionLimit must be greater than zero" }
        require(allowedCategories.isNotEmpty()) { "allowedCategories must not be empty" }
    }
}

enum class FilePickerPurpose(val defaultSelectionLimit: Int) {
    ChatAttachment(9),
    Import(9),
}

enum class FileCategory { All, Document, Image, Video, Audio, Archive, Other }

enum class FileSort { Name, ModifiedTime, Size }
