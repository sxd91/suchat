package io.github.sxd91.suchat.feature.media.model

/** Ordered selection state. LinkedHashSet preserves the order in which items were selected. */
class SelectionState<T>(
    val selectionLimit: Int,
    selected: Collection<T> = emptyList(),
) {
    private val orderedSelection = LinkedHashSet<T>()

    init {
        require(selectionLimit > 0) { "selectionLimit must be greater than zero" }
        selected.forEach { item ->
            require(orderedSelection.size < selectionLimit || item in orderedSelection) {
                "Initial selection exceeds selectionLimit"
            }
            orderedSelection += item
        }
    }

    val items: Set<T> get() = orderedSelection.toSet()
    val count: Int get() = orderedSelection.size
    val isAtLimit: Boolean get() = count >= selectionLimit

    fun isSelected(item: T): Boolean = item in orderedSelection
    fun selectionIndex(item: T): Int? = orderedSelection.indexOf(item).takeIf { it >= 0 }?.plus(1)

    fun toggle(item: T): SelectionChange<T> = when {
        orderedSelection.remove(item) -> SelectionChange.Removed(item)
        isAtLimit -> SelectionChange.LimitReached(item, selectionLimit)
        else -> {
            orderedSelection += item
            SelectionChange.Added(item, count)
        }
    }

    fun clear() = orderedSelection.clear()
}

sealed interface SelectionChange<T> {
    data class Added<T>(val item: T, val selectionIndex: Int) : SelectionChange<T>
    data class Removed<T>(val item: T) : SelectionChange<T>
    data class LimitReached<T>(val item: T, val selectionLimit: Int) : SelectionChange<T>
}
