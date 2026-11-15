package com.example.starter.feature.quiz.domain

data class Question(val id: Int, val text: String, val options: List<String>, val correctIndex: Int)

data class Quiz(val id: Int, val title: String, val durationSeconds: Int, val questions: List<Question>)

data class AnswerReview(val question: Question, val selectedIndex: Int?) {
    val isCorrect: Boolean get() = selectedIndex == question.correctIndex
}

data class QuizResult(val reviews: List<AnswerReview>, val timedOut: Boolean) {
    val score: Int get() = reviews.count { it.isCorrect }
    val total: Int get() = reviews.size
    val unanswered: Int get() = reviews.count { it.selectedIndex == null }
}

/** Pure grading: answers are indexed by question position, null = skipped. */
fun Quiz.grade(answers: List<Int?>, timedOut: Boolean): QuizResult =
    QuizResult(questions.mapIndexed { i, q -> AnswerReview(q, answers.getOrNull(i)) }, timedOut)
