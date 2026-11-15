package com.example.starter.feature.comments.domain

data class CommentRow(
    val comment: Comment,
    val depth: Int,
    /** Visible replies under this comment (all levels). */
    val replyCount: Int,
    val isCollapsed: Boolean
)

/**
 * Turns the tree into the flat list a LazyColumn needs: pre-order DFS, siblings by score (desc),
 * hidden comments removed, collapsed comments shown without their replies.
 *
 * No recursion anywhere (explicit stack + bottom-up counting), so a 20,000-deep reply chain
 * can't overflow the call stack. Comments whose parent is missing (deleted) are treated as top-level.
 */
object ThreadFlattener {

    fun flatten(comments: List<Comment>, hidden: Set<Int>, collapsed: Set<Int>): List<CommentRow> {
        val visible = comments.filter { it.id !in hidden }
        val byId = visible.associateBy { it.id }
        val children = visible.filter { it.parentId != null && it.parentId in byId }
            .groupBy { it.parentId!! }
            .mapValues { (_, list) -> list.sortedByDescending { it.score } }
        val roots = visible.filter { it.parentId == null || it.parentId !in byId }.sortedByDescending { it.score }

        val fullOrder = preOrder(roots, children, collapsed = emptySet()).map { it.first }
        val replyCounts = HashMap<Int, Int>(fullOrder.size)
        // Reverse pre-order visits every child before its parent: count bottom-up.
        for (comment in fullOrder.asReversed()) {
            replyCounts[comment.id] = children[comment.id].orEmpty().sumOf { 1 + (replyCounts[it.id] ?: 0) }
        }

        return preOrder(roots, children, collapsed).map { (comment, depth) ->
            CommentRow(comment, depth, replyCounts[comment.id] ?: 0, isCollapsed = comment.id in collapsed)
        }
    }

    /** Iterative pre-order DFS. The visited set guards against cycles in malformed data. */
    private fun preOrder(roots: List<Comment>, children: Map<Int, List<Comment>>, collapsed: Set<Int>): List<Pair<Comment, Int>> {
        val out = ArrayList<Pair<Comment, Int>>()
        val visited = HashSet<Int>()
        val stack = ArrayDeque<Pair<Comment, Int>>()
        roots.asReversed().forEach { stack.addLast(it to 0) }
        while (stack.isNotEmpty()) {
            val (comment, depth) = stack.removeLast()
            if (!visited.add(comment.id)) continue
            out += comment to depth
            if (comment.id !in collapsed) children[comment.id].orEmpty().asReversed().forEach { stack.addLast(it to depth + 1) }
        }
        return out
    }
}
