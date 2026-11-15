package com.example.starter.feature.comments.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CommentFilterTest {
    private fun c(id: Int, parent: Int?, label: PetLabel, score: Int = 0) = Comment(id, parent, "u$id", "t$id", label, score)

    //   1 DOG
    //   ├─ 2 CAT (score 40)
    //   │  └─ 3 DOG
    //   │     └─ 4 CAT (score 99)
    //   └─ 5 DOG
    //   6 CAT
    //   └─ 7 DOG
    private val thread = listOf(
        c(1, null, PetLabel.DOG),
        c(2, 1, PetLabel.CAT, 40),
        c(3, 2, PetLabel.DOG),
        c(4, 3, PetLabel.CAT, 99),
        c(5, 1, PetLabel.DOG),
        c(6, null, PetLabel.CAT),
        c(7, 6, PetLabel.DOG)
    )

    @Test
    fun `dog person hides cat comments and their whole subtree`() {
        assertThat(CommentFilter.hiddenIds(thread, Preference.DOG_PERSON)).containsExactly(2, 3, 4, 6, 7)
    }

    @Test
    fun `cat person hides dog comments and their whole subtree`() {
        assertThat(CommentFilter.hiddenIds(thread, Preference.CAT_PERSON)).containsExactly(1, 2, 3, 4, 5, 7)
    }

    @Test
    fun `show all hides nothing`() {
        assertThat(CommentFilter.hiddenIds(thread, Preference.SHOW_ALL)).isEmpty()
    }

    @Test
    fun `follow-up - high score rescues the comment but not when an ancestor is hidden`() {
        // 2 (40 pts) is rescued at threshold 30, so its subtree is evaluated normally:
        // 4 (99 pts) is rescued too. 6 (0 pts) is still hidden with its child 7.
        assertThat(CommentFilter.hiddenIds(thread, Preference.DOG_PERSON, scoreThreshold = 30)).containsExactly(6, 7)
        // At threshold 50, 2 is hidden again, and 4 can't be rescued because its ancestor is hidden.
        assertThat(CommentFilter.hiddenIds(thread, Preference.DOG_PERSON, scoreThreshold = 50)).containsExactly(2, 3, 4, 6, 7)
    }

    @Test
    fun `malformed data with a cycle terminates`() {
        val cyclic = listOf(c(1, 2, PetLabel.CAT), c(2, 1, PetLabel.DOG))
        assertThat(CommentFilter.hiddenIds(cyclic, Preference.DOG_PERSON)).containsExactly(1, 2)
    }
}
