package com.example.starter.feature.quiz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starter.R
import com.example.starter.core.ui.CenteredLoader
import com.example.starter.core.ui.ErrorView
import com.example.starter.feature.quiz.domain.QuizResult

@Composable
fun QuizRoute(viewModel: QuizViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                QuizEffect.TimeUp -> snackbar.showSnackbar(context.getString(R.string.quiz_time_up))
            }
        }
    }
    QuizScreen(state, snackbar, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(state: QuizState, snackbar: SnackbarHostState, onIntent: (QuizIntent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.quiz?.title ?: stringResource(R.string.quiz_title)) },
                actions = { if (state.quiz != null && !state.isSubmitted) Countdown(state.remainingSeconds) }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CenteredLoader()
                state.error != null -> ErrorView(state.error.asString()) { onIntent(QuizIntent.Retry) }
                state.result != null -> ResultView(state.result) { onIntent(QuizIntent.Retake) }
                state.quiz != null -> QuestionView(state, onIntent)
            }
        }
    }
}

@Composable
private fun Countdown(seconds: Int) {
    val urgent = seconds <= URGENT_SECONDS
    Text(
        "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(end = 16.dp)
    )
}

@Composable
private fun QuestionView(state: QuizState, onIntent: (QuizIntent) -> Unit) {
    val quiz = state.quiz ?: return
    val question = quiz.questions[state.currentIndex]
    var confirmSubmit by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ProgressDots(state, onIntent)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.quiz_question_of, state.currentIndex + 1, quiz.questions.size),
            style = MaterialTheme.typography.labelLarge
        )
        Text(question.text, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        question.options.forEachIndexed { i, option ->
            Row(
                Modifier.fillMaxWidth().clickable { onIntent(QuizIntent.SelectOption(i)) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = state.answers[state.currentIndex] == i, onClick = { onIntent(QuizIntent.SelectOption(i)) })
                Text(option)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onIntent(QuizIntent.Previous) }, enabled = state.currentIndex > 0) {
                Text(stringResource(R.string.quiz_previous))
            }
            Spacer(Modifier.weight(1f))
            if (state.isLastQuestion) {
                Button(onClick = { if (state.unansweredCount > 0) confirmSubmit = true else onIntent(QuizIntent.Submit) }) {
                    Text(stringResource(R.string.quiz_submit))
                }
            } else {
                Button(onClick = { onIntent(QuizIntent.Next) }) { Text(stringResource(R.string.quiz_next)) }
            }
        }
    }
    if (confirmSubmit) {
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text(stringResource(R.string.quiz_confirm_title)) },
            text = { Text(stringResource(R.string.quiz_confirm_body, state.unansweredCount)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmSubmit = false
                    onIntent(QuizIntent.Submit)
                }) { Text(stringResource(R.string.quiz_submit)) }
            },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text(stringResource(R.string.quiz_keep_going)) } }
        )
    }
}

@Composable
private fun ProgressDots(state: QuizState, onIntent: (QuizIntent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.answers.forEachIndexed { i, answer ->
            val color = when {
                i == state.currentIndex -> MaterialTheme.colorScheme.primary
                answer != null -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Box(Modifier.size(DOT_SIZE.dp).clip(CircleShape).background(color).clickable { onIntent(QuizIntent.JumpTo(i)) })
        }
    }
}

@Composable
private fun ResultView(result: QuizResult, onRetake: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(stringResource(R.string.quiz_score, result.score, result.total), style = MaterialTheme.typography.headlineMedium)
            if (result.timedOut) Text(stringResource(R.string.quiz_time_up), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetake) { Text(stringResource(R.string.quiz_retake)) }
        }
        itemsIndexed(result.reviews) { i, review ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("${i + 1}. ${review.question.text}", style = MaterialTheme.typography.titleMedium)
                    val yours = review.selectedIndex?.let { review.question.options[it] } ?: stringResource(R.string.quiz_skipped)
                    Text(
                        stringResource(R.string.quiz_your_answer, yours),
                        color = if (review.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    if (!review.isCorrect) {
                        Text(stringResource(R.string.quiz_correct_answer, review.question.options[review.question.correctIndex]))
                    }
                }
            }
        }
    }
}

private const val URGENT_SECONDS = 10
private const val SECONDS_PER_MINUTE = 60
private const val DOT_SIZE = 14
