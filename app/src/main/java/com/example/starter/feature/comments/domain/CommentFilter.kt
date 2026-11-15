package com.example.starter.feature.comments.domain

/**
 * The reported coding question, as a pure function.
 *
 * A comment is "excluded" when its label is the one the user dislikes. An excluded comment is hidden
 * together with ALL of its descendants (replies to a hidden comment make no sense on their own).
 *
 * Follow-up: comments scoring above [scoreThreshold] are "rescued" and stay visible even if excluded.
 * A rescued comment under a hidden ancestor is still hidden: hiding a subtree always wins.
 *
 * Complexity: O(n) time and space. Children map + BFS with a visited set
 * (the visited set also protects against malformed data with cycles).
 */
object CommentFilter {

    fun hiddenIds(comments: List<Comment>, preference: Preference, scoreThreshold: Int? = null): Set<Int> {
        val excluded = preference.excluded ?: return emptySet()
        val children: Map<Int, List<Int>> = comments
            .filter { it.parentId != null }
            .groupBy({ it.parentId!! }, { it.id })

        val hidden = HashSet<Int>()
        comments
            .filter { it.label == excluded && !isRescued(it, scoreThreshold) }
            .forEach { hideSubtree(it.id, children, hidden) }
        return hidden
    }

    private fun isRescued(comment: Comment, scoreThreshold: Int?): Boolean = scoreThreshold != null && comment.score > scoreThreshold

    /** BFS from [rootId]; already-hidden nodes are skipped, which also stops cycles. */
    private fun hideSubtree(rootId: Int, children: Map<Int, List<Int>>, hidden: MutableSet<Int>) {
        val queue = ArrayDeque(listOf(rootId))
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (hidden.add(id)) children[id]?.let(queue::addAll)
        }
    }
}
