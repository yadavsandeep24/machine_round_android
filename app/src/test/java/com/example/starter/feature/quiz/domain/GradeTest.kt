package com.example.starter.feature.quiz.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GradeTest {
    private val quiz = Quiz(
        1,
        "t",
        60,
        listOf(Question(1, "a", listOf("x", "y"), 0), Question(2, "b", listOf("x", "y"), 1), Question(3, "c", listOf("x", "y"), 1))
    )

    @Test
    fun `counts correct, wrong and skipped`() {
        val result = quiz.grade(listOf(0, 0, null), timedOut = false)
        assertThat(result.score).isEqualTo(1)
        assertThat(result.total).isEqualTo(3)
        assertThat(result.unanswered).isEqualTo(1)
    }
}
