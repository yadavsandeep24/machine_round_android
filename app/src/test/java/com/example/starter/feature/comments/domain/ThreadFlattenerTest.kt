package com.example.starter.feature.comments.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThreadFlattenerTest {
    private fun c(id: Int, parent: Int?, score: Int) = Comment(id, parent, "u$id", "t$id", PetLabel.DOG, score)

    private val thread = listOf(
        c(1, null, 10),
        c(2, 1, 1),
        c(3, 1, 5),
        c(4, 3, 0),
        c(5, null, 50),
        c(6, 999, 7) // parent deleted
    )

    @Test
    fun `pre-order, siblings by score, depth, orphans as roots`() {
        val rows = ThreadFlattener.flatten(thread, hidden = emptySet(), collapsed = emptySet())
        assertThat(rows.map { it.comment.id }).containsExactly(5, 1, 3, 4, 2, 6).inOrder()
        assertThat(rows.map { it.depth }).containsExactly(0, 0, 1, 2, 1, 0).inOrder()
        assertThat(rows.first { it.comment.id == 1 }.replyCount).isEqualTo(3)
    }

    @Test
    fun `collapsed comment stays, its replies are skipped`() {
        val rows = ThreadFlattener.flatten(thread, hidden = emptySet(), collapsed = setOf(1))
        assertThat(rows.map { it.comment.id }).containsExactly(5, 1, 6).inOrder()
        assertThat(rows.first { it.comment.id == 1 }.isCollapsed).isTrue()
    }

    @Test
    fun `hidden comments are removed`() {
        val rows = ThreadFlattener.flatten(thread, hidden = setOf(3, 4), collapsed = emptySet())
        assertThat(rows.map { it.comment.id }).containsExactly(5, 1, 2, 6).inOrder()
    }

    @Test
    fun `very deep thread does not overflow the stack`() {
        val deep = (1..20_000).map { c(it, if (it == 1) null else it - 1, 0) }
        assertThat(ThreadFlattener.flatten(deep, emptySet(), emptySet())).hasSize(20_000)
    }
}
