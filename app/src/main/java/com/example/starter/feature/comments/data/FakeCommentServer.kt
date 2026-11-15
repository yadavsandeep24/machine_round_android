package com.example.starter.feature.comments.data

import com.example.starter.feature.comments.domain.Comment
import com.example.starter.feature.comments.domain.PetLabel
import java.io.IOException
import kotlinx.coroutines.delay

/** In-app stand-in for the comments API: a deterministic, nested dog-vs-cat thread. */
class FakeCommentServer(private val latencyMs: Long = DEFAULT_LATENCY_MS) {
    var failuresRemaining = 0

    suspend fun fetchComments(@Suppress("UNUSED_PARAMETER") postId: Int): List<Comment> {
        delay(latencyMs)
        if (failuresRemaining > 0) {
            failuresRemaining--
            throw IOException("Simulated network failure")
        }
        return THREAD
    }

    private companion object {
        const val DEFAULT_LATENCY_MS = 500L
        private fun dog(id: Int, parent: Int?, score: Int, text: String) = Comment(id, parent, "u/doglover$id", text, PetLabel.DOG, score)
        private fun cat(id: Int, parent: Int?, score: Int, text: String) = Comment(id, parent, "u/catfan$id", text, PetLabel.CAT, score)

        val THREAD = listOf(
            dog(1, null, 120, "Dogs are the best co-workers. Mine attends every stand-up."),
            cat(2, 1, 45, "Cats attend stand-ups too, they just sit on the keyboard."),
            dog(3, 2, 12, "That's not attending, that's sabotage."),
            cat(4, 3, 8, "Sabotage is a valid engineering practice."),
            dog(5, 1, 30, "Golden retrievers would ship on time."),
            cat(6, null, 210, "Unpopular opinion: cats are self-documenting."),
            dog(7, 6, 64, "Self-documenting? They never explain anything."),
            cat(8, 7, 5, "Exactly. Like good code, you just observe it."),
            cat(9, 6, 2, "My cat reviewed my PR by deleting it."),
            dog(10, 9, 3, "Sounds like a strict reviewer."),
            dog(11, null, 15, "Dog people, what's your pup's name?"),
            dog(12, 11, 9, "Kotlin. Obviously."),
            cat(13, 11, 70, "Mine's a cat named Compose. Recomposes every nap."),
            dog(14, 13, 4, "That's a cute name though."),
            cat(15, null, 1, "Cats > dogs, no further questions."),
            dog(16, 15, 22, "Objection!"),
            cat(17, 16, 6, "Overruled."),
            dog(18, null, 55, "Both are great, but dogs fetch bugs faster."),
            cat(19, 18, 11, "Cats don't fetch bugs, they hunt them."),
            dog(20, 19, 2, "Same result, more drama."),
            // Reply to a deleted parent (id 999): treated as top-level.
            cat(21, 999, 3, "Replying to a deleted comment, as one does.")
        )
    }
}
