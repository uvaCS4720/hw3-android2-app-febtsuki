package edu.nd.pmcburne.hwapp.one

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.nd.pmcburne.hwapp.one.data.BasketballRepository
import edu.nd.pmcburne.hwapp.one.data.local.GameEntity
import edu.nd.pmcburne.hwapp.one.data.local.ScoreDatabase
import edu.nd.pmcburne.hwapp.one.ui.ScoreViewModel
import edu.nd.pmcburne.hwapp.one.ui.theme.HWStarterRepoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = BasketballRepository.create(
            ScoreDatabase.getInstance(applicationContext).gameDao()
        )

        enableEdgeToEdge()
        setContent {
            HWStarterRepoTheme {
                val viewModel: ScoreViewModel = viewModel(
                    factory = ScoreViewModel.factory(repository)
                )
                ScoresApp(viewModel)
            }
        }
    }
}

@Composable
fun ScoresApp(viewModel: ScoreViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                TopHeader(
                    selectedDate = uiState.selectedDate,
                    selectedGender = uiState.selectedGender,
                    lastUpdatedText = uiState.lastUpdatedText,
                    onPickDate = viewModel::onDateSelected,
                    onPreviousDay = viewModel::goToPreviousDay,
                    onNextDay = viewModel::goToNextDay,
                    onRefresh = viewModel::refresh,
                    onGenderSelected = viewModel::onGenderSelected
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.games.isEmpty() && !uiState.isLoading) {
                    EmptyState(
                        hasAttemptedRefresh = uiState.hasAttemptedRefresh,
                        onRefresh = viewModel::refresh
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.games,
                            key = { "${it.gameId}-${it.gender}-${it.gameDate}" }
                        ) { game ->
                            GameCard(
                                game = game,
                                gender = uiState.selectedGender
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                LoadingOverlay()
            }
        }
    }
}

@Composable
private fun TopHeader(
    selectedDate: LocalDate,
    selectedGender: String,
    lastUpdatedText: String?,
    onPickDate: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onRefresh: () -> Unit,
    onGenderSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NCAA Basketball Scores",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (selectedGender == "men") {
                            "Men's Division I games"
                        } else {
                            "Women's Division I games"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }

                    Text(
                        text = "Refresh",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = lastUpdatedText?.let { "Last updated $it" } ?: "Not updated yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousDay) {
                        Text(
                            text = "<",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AssistChip(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    onPickDate(LocalDate.of(year, month + 1, dayOfMonth))
                                },
                                selectedDate.year,
                                selectedDate.monthValue - 1,
                                selectedDate.dayOfMonth
                            ).show()
                        },
                        label = {
                            Text(
                                text = selectedDate.format(formatter),
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select date"
                            )
                        }
                    )

                    IconButton(onClick = onNextDay) {
                        Text(
                            text = ">",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedGender == "men",
                    onClick = { onGenderSelected("men") },
                    label = {
                        Text(
                            text = "Men",
                            fontWeight = if (selectedGender == "men") FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )

                FilterChip(
                    selected = selectedGender == "women",
                    onClick = { onGenderSelected("women") },
                    label = {
                        Text(
                            text = "Women",
                            fontWeight = if (selectedGender == "women") FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun GameCard(
    game: GameEntity,
    gender: String
) {
    val statusText = buildStatusText(game, gender)
    val badgeText = when {
        isFinal(game) -> "FINAL"
        isUpcoming(game) -> "UPCOMING"
        else -> "LIVE"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Matchup",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${game.awayTeam} at ${game.homeTeam}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                StatusBadge(badgeText = badgeText)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TeamScoreRow(
                    teamName = game.awayTeam,
                    isHome = false,
                    score = game.awayScore,
                    isWinner = game.awayWinner
                )
                TeamScoreRow(
                    teamName = game.homeTeam,
                    isHome = true,
                    score = game.homeScore,
                    isWinner = game.homeWinner
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (isFinal(game) && (game.homeWinner || game.awayWinner)) {
                        Text(
                            text = "Winner: ${if (game.homeWinner) game.homeTeam else game.awayTeam}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamScoreRow(
    teamName: String,
    isHome: Boolean,
    score: Int?,
    isWinner: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isWinner) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHome) "Home" else "Away",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isWinner) "$teamName  ✓" else teamName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium
                )
            }

            Text(
                text = score?.toString() ?: "-",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun StatusBadge(badgeText: String) {
    val backgroundColor = when (badgeText) {
        "LIVE" -> MaterialTheme.colorScheme.primary
        "FINAL" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    val textColor = when (badgeText) {
        "LIVE" -> MaterialTheme.colorScheme.onPrimary
        "FINAL" -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onTertiary
    }

    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = badgeText,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyState(
    hasAttemptedRefresh: Boolean,
    onRefresh: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasAttemptedRefresh) "No games found" else "Loading games",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasAttemptedRefresh) {
                    "Try another date, switch between men's and women's games, or refresh."
                } else {
                    "Fetching the latest scores for you."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onRefresh) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Loading scores...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun buildStatusText(game: GameEntity, gender: String): String {
    return when {
        isUpcoming(game) -> {
            "Upcoming • ${formatStartTime(game.startTimeText)}"
        }
        isFinal(game) -> {
            "Final"
        }
        else -> {
            val periodLabel = formatPeriod(game.currentPeriod, gender)
            val clock = if (game.contestClock.isBlank() || game.contestClock == "0:00") {
                ""
            } else {
                " • ${game.contestClock}"
            }
            "Live • $periodLabel$clock"
        }
    }
}

private fun formatStartTime(startTimeText: String): String {
    return if (startTimeText.isBlank()) "Start time unavailable" else startTimeText
}

private fun isUpcoming(game: GameEntity): Boolean {
    return game.gameState.equals("pre", ignoreCase = true)
}

private fun isFinal(game: GameEntity): Boolean {
    return game.gameState.equals("final", ignoreCase = true)
}

private fun formatPeriod(currentPeriod: String, gender: String): String {
    if (currentPeriod.isBlank()) return "In Progress"
    if (currentPeriod.equals("HALFTIME", ignoreCase = true)) return "Halftime"
    if (currentPeriod.equals("FINAL", ignoreCase = true)) return "Final"

    return if (gender == "women") {
        when (currentPeriod.lowercase()) {
            "1st" -> "Q1"
            "2nd" -> "Q2"
            "3rd" -> "Q3"
            "4th" -> "Q4"
            else -> currentPeriod
        }
    } else {
        when (currentPeriod.lowercase()) {
            "1st" -> "1st Half"
            "2nd" -> "2nd Half"
            else -> currentPeriod
        }
    }
}