package com.littlelogic.sudoku

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.random.Random

private val Ink = Color(0xFF233A43)
private val Muted = Color(0xFF84939A)
private val Teal = Color(0xFF277D74)
private val TealDark = Color(0xFF17685F)
private val Paper = Color(0xFFF7F5EE)
private val PaleGreen = Color(0xFFE4F1EB)
private val GridLine = Color(0xFFDCE7E3)
private val Coral = Color(0xFFED9273)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(247, 245, 238)
        window.navigationBarColor = android.graphics.Color.rgb(247, 245, 238)
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { SudokuApp() }
    }
}

private data class GameState(
    val level: Int = 1,
    val difficulty: String = "easy",
    val solution: IntArray = IntArray(81),
    val puzzle: IntArray = IntArray(81),
    val values: IntArray = IntArray(81),
    val notes: IntArray = IntArray(81),
    val selected: Int = 40,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val seconds: Int = 0,
    val status: String = "playing",
    val noteMode: Boolean = false,
)

private data class BoardSnapshot(val values: IntArray, val notes: IntArray)

private object SudokuEngine {
    private fun shuffled(values: List<Int>) = values.shuffled(Random)
    private fun box(row: Int, col: Int) = row / 3 * 3 + col / 3

    private fun solvedGrid(): IntArray {
        val rowGroups = shuffled(listOf(0, 1, 2))
        val colGroups = shuffled(listOf(0, 1, 2))
        val rows = rowGroups.flatMap { group -> shuffled(listOf(0, 1, 2)).map { group * 3 + it } }
        val cols = colGroups.flatMap { group -> shuffled(listOf(0, 1, 2)).map { group * 3 + it } }
        val digits = shuffled((1..9).toList())
        return IntArray(81) { index ->
            val row = rows[index / 9]
            val col = cols[index % 9]
            digits[(row * 3 + row / 3 + col) % 9]
        }
    }

    private fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        val rows = IntArray(9)
        val cols = IntArray(9)
        val boxes = IntArray(9)
        val full = 0x3FE
        for (i in grid.indices) {
            val value = grid[i]
            if (value == 0) continue
            val bit = 1 shl value
            val row = i / 9
            val col = i % 9
            val box = box(row, col)
            if (((rows[row] or cols[col] or boxes[box]) and bit) != 0) return 0
            rows[row] = rows[row] or bit
            cols[col] = cols[col] or bit
            boxes[box] = boxes[box] or bit
        }

        var count = 0
        fun search() {
            if (count >= limit) return
            var best = -1
            var bestMask = 0
            var bestCount = 10
            for (i in grid.indices) {
                if (grid[i] != 0) continue
                val row = i / 9
                val col = i % 9
                val boxIndex = box(row, col)
                val mask = full and (rows[row] or cols[col] or boxes[boxIndex]).inv()
                val candidates = Integer.bitCount(mask)
                if (candidates == 0) return
                if (candidates < bestCount) {
                    best = i
                    bestMask = mask
                    bestCount = candidates
                    if (candidates == 1) break
                }
            }
            if (best == -1) {
                count++
                return
            }
            val row = best / 9
            val col = best % 9
            val boxIndex = box(row, col)
            var candidates = bestMask
            while (candidates != 0 && count < limit) {
                val bit = candidates and -candidates
                candidates = candidates xor bit
                val digit = Integer.numberOfTrailingZeros(bit)
                grid[best] = digit
                rows[row] = rows[row] or bit
                cols[col] = cols[col] or bit
                boxes[boxIndex] = boxes[boxIndex] or bit
                search()
                grid[best] = 0
                rows[row] = rows[row] xor bit
                cols[col] = cols[col] xor bit
                boxes[boxIndex] = boxes[boxIndex] xor bit
            }
        }
        search()
        return count
    }

    fun newPuzzle(difficulty: String): Pair<IntArray, IntArray> {
        val solution = solvedGrid()
        val puzzle = solution.copyOf()
        val targetClues = when (difficulty) {
            "medium" -> 35
            "hard" -> 29
            else -> 42
        }
        var clues = 81
        for (index in (0..80).shuffled(Random)) {
            if (clues <= targetClues) break
            val previous = puzzle[index]
            puzzle[index] = 0
            if (countSolutions(puzzle.copyOf()) == 1) clues-- else puzzle[index] = previous
        }
        return solution to puzzle
    }

    fun arePeers(first: Int, second: Int): Boolean =
        first / 9 == second / 9 || first % 9 == second % 9 || box(first / 9, first % 9) == box(second / 9, second % 9)
}

@Composable
private fun SudokuApp() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val interstitialController = remember(activity) { activity?.let { YandexInterstitialController(it) } }
    val preferences = remember { context.getSharedPreferences("littlelogic-sudoku", 0) }
    var language by remember { mutableStateOf(preferences.getString("language", "ru") ?: "ru") }
    var game by remember { mutableStateOf(loadGame(preferences) ?: createGame(1, "easy")) }
    var dialog by remember { mutableStateOf(game.status.takeIf { it == "won" || it == "lost" }) }
    var hintCell by remember { mutableStateOf(-1) }
    var advancingLevel by remember { mutableStateOf(false) }
    val history = remember { ArrayDeque<BoardSnapshot>() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember { mutableStateOf(true) }
    val russian = language == "ru"

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) isForeground = true
            if (event == Lifecycle.Event.ON_STOP) isForeground = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(interstitialController) {
        interstitialController?.preload()
        onDispose { interstitialController?.destroy() }
    }

    LaunchedEffect(game.status, dialog, isForeground) {
        while (game.status == "playing" && dialog == null && isForeground) {
            delay(1000)
            game = game.copy(seconds = (game.seconds + 1).coerceAtMost(359_999))
        }
    }
    LaunchedEffect(game, language) {
        saveGame(preferences, game)
        preferences.edit().putString("language", language).apply()
    }
    LaunchedEffect(hintCell) {
        if (hintCell >= 0) {
            delay(700)
            hintCell = -1
        }
    }

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }
    fun startNew(difficulty: String = game.difficulty, advance: Boolean = false) {
        history.clear()
        val nextLevel = if (advance) (game.level + 1).coerceAtMost(999_999) else game.level
        game = createGame(nextLevel, difficulty)
        dialog = null
    }
    fun proceedToNextLevel() {
        if (advancingLevel) return
        advancingLevel = true
        val continueToNextLevel = {
            startNew(advance = true)
            advancingLevel = false
        }
        if (interstitialController != null) {
            interstitialController.showBetweenLevels(continueToNextLevel)
        } else {
            continueToNextLevel()
        }
    }
    fun snapshot() {
        history.addLast(BoardSnapshot(game.values.copyOf(), game.notes.copyOf()))
        while (history.size > 100) history.removeFirst()
    }
    fun enterNumber(number: Int) {
        if (game.status != "playing" || dialog != null) return
        val index = game.selected
        if (game.puzzle[index] != 0) {
            notify(if (russian) "Эта цифра уже задана" else "This number is already set")
            return
        }
        if (game.noteMode) {
            if (game.values[index] != 0) return
            snapshot()
            val notes = game.notes.copyOf()
            notes[index] = notes[index] xor (1 shl (number - 1))
            game = game.copy(notes = notes)
            return
        }
        if (game.values[index] == number) return
        snapshot()
        val values = game.values.copyOf()
        val notes = game.notes.copyOf()
        values[index] = number
        notes[index] = 0
        if (number == game.solution[index]) {
            for (peer in 0..80) if (SudokuEngine.arePeers(index, peer)) {
                notes[peer] = notes[peer] and (1 shl (number - 1)).inv()
            }
        }
        val mistakes = game.mistakes + if (number == game.solution[index]) 0 else 1
        val completed = values.contentEquals(game.solution)
        game = game.copy(values = values, notes = notes, mistakes = mistakes, status = when {
            completed -> "won"
            mistakes >= 3 -> "lost"
            else -> "playing"
        })
        if (game.status != "playing") dialog = game.status
    }
    fun eraseSelected() {
        val index = game.selected
        if (game.status != "playing" || dialog != null) return
        if (game.puzzle[index] != 0) {
            notify(if (russian) "Эта цифра уже задана" else "This number is already set")
            return
        }
        if (game.values[index] == 0 && game.notes[index] == 0) return
        snapshot()
        val values = game.values.copyOf().also { it[index] = 0 }
        val notes = game.notes.copyOf().also { it[index] = 0 }
        game = game.copy(values = values, notes = notes)
    }
    fun undo() {
        if (game.status != "playing" || dialog != null) return
        val previous = history.removeLastOrNull()
        if (previous == null) {
            notify(if (russian) "Пока нечего отменять" else "No move to undo yet")
            return
        }
        game = game.copy(values = previous.values, notes = previous.notes)
    }
    fun useHint() {
        if (game.status != "playing" || dialog != null) return
        if (game.hintsUsed >= 3) {
            notify(if (russian) "Подсказки на сегодня закончились" else "No hints left for this puzzle")
            return
        }
        var index = game.selected
        if (game.puzzle[index] != 0 || game.values[index] == game.solution[index]) {
            index = game.values.indices.firstOrNull { game.values[it] != game.solution[it] } ?: -1
        }
        if (index < 0) return
        snapshot()
        val values = game.values.copyOf().also { it[index] = game.solution[index] }
        val notes = game.notes.copyOf().also { it[index] = 0 }
        for (peer in 0..80) if (SudokuEngine.arePeers(index, peer)) {
            notes[peer] = notes[peer] and (1 shl (game.solution[index] - 1)).inv()
        }
        hintCell = index
        game = game.copy(values = values, notes = notes, selected = index, hintsUsed = game.hintsUsed + 1,
            status = if (values.contentEquals(game.solution)) "won" else "playing")
        if (game.status == "won") dialog = "won"
    }

    MaterialTheme {
        Scaffold(
            containerColor = Paper,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Surface(color = Color.White) {
                    YandexBanner(Modifier.navigationBarsPadding())
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header(language) { selected ->
                    language = selected
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (russian) "НАЙДИ СВОЙ РИТМ" else "A MOMENT FOR YOU", color = Teal,
                            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        Text(if (russian) "Время для судоку" else "A little sudoku pause", color = Ink,
                            fontSize = 25.sp, fontFamily = FontFamily.Serif, lineHeight = 30.sp)
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = PaleGreen) {
                        Column(Modifier.padding(horizontal = 13.dp, vertical = 7.dp), horizontalAlignment = Alignment.End) {
                            Text(if (russian) "УРОВЕНЬ" else "LEVEL", fontSize = 9.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(game.level.toString().padStart(2, '0'), fontSize = 17.sp, color = TealDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                StatsRow(game, russian)
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (russian) "ТВОЯ ГОЛОВОЛОМКА" else "YOUR PUZZLE", fontSize = 9.sp,
                                    color = Muted, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
                                Text(if (russian) "Судоку 9 × 9" else "Sudoku 9 × 9", fontSize = 19.sp,
                                    color = Ink, fontFamily = FontFamily.Serif)
                            }
                            TextButton(onClick = { dialog = if (game.status == "playing") "confirm" else "new" }) {
                                Text("↻  ${if (russian) "Новая игра" else "New game"}", color = Teal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        DifficultySelector(game.difficulty, russian) { selected ->
                            if (selected != game.difficulty) startNew(selected)
                        }
                        Spacer(Modifier.height(12.dp))
                        SudokuBoard(game, hintCell) { index ->
                            if (game.status == "playing" && dialog == null) game = game.copy(selected = index)
                        }
                        Spacer(Modifier.height(11.dp))
                        NumberPad(game) { number -> enterNumber(number) }
                        Spacer(Modifier.height(11.dp))
                        ProgressLine(game, russian)
                    }
                }
                Spacer(Modifier.height(13.dp))
                ToolsRow(game, russian, onNotes = {
                    if (game.status == "playing") game = game.copy(noteMode = !game.noteMode)
                }, onHint = { useHint() }, onErase = { eraseSelected() }, onUndo = { undo() })
                Spacer(Modifier.height(13.dp))
                Text(
                    if (russian) "Строка, столбец и квадрат 3 × 3 содержат цифры от 1 до 9." else "Every row, column, and 3 × 3 box uses the numbers 1 to 9.",
                    color = Muted, textAlign = TextAlign.Center, fontSize = 11.sp, lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    when (dialog) {
        "confirm", "new" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(if (russian) "Начнём новую игру?" else "Start a new game?", fontFamily = FontFamily.Serif) },
            text = { Text(if (russian) "Можно выбрать другой уровень сложности и продолжить в своём темпе." else "Choose a difficulty and keep enjoying the puzzle.") },
            confirmButton = { TextButton(onClick = { startNew() }) { Text(if (russian) "Начать" else "Start", color = Teal) } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(if (russian) "Продолжить" else "Keep playing", color = Muted) } },
            containerColor = Color.White,
        )
        "won" -> AlertDialog(
            onDismissRequest = { },
            title = { Text(if (russian) "Блестящая работа! ✨" else "Beautifully done! ✨", fontFamily = FontFamily.Serif) },
            text = { Text(if (russian) "Головоломка решена за ${formatTime(game.seconds)}. Отличный повод для маленькой паузы." else "Puzzle complete in ${formatTime(game.seconds)}. A lovely little win.") },
            confirmButton = { TextButton(enabled = !advancingLevel, onClick = { proceedToNextLevel() }) { Text(if (russian) "Следующий уровень" else "Next level", color = Teal) } },
            dismissButton = { TextButton(onClick = { startNew() }) { Text(if (russian) "Ещё одну" else "Play again", color = Muted) } },
            containerColor = Color.White,
        )
        "lost" -> AlertDialog(
            onDismissRequest = { },
            title = { Text(if (russian) "Новый шанс?" else "Ready for another try?", fontFamily = FontFamily.Serif) },
            text = { Text(if (russian) "Новая головоломка уже ждёт. Можно начать заново в удобном темпе." else "A fresh puzzle is ready whenever you are.") },
            confirmButton = { TextButton(onClick = { startNew() }) { Text(if (russian) "Попробовать снова" else "Try again", color = Teal) } },
            dismissButton = { TextButton(onClick = { startNew() }) { Text(if (russian) "Новая игра" else "New game", color = Muted) } },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun Header(language: String, onLanguageChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(38.dp), shape = RoundedCornerShape(12.dp), color = Teal) {
            Box(contentAlignment = Alignment.Center) {
                Text("▦", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(9.dp))
        Text("Little Logic", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Ink)
        Text(" Sudoku", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Teal)
        Spacer(Modifier.weight(1f))
        Row(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFEBECE6)).padding(3.dp)) {
            listOf("ru" to "RU", "en" to "EN").forEach { (code, label) ->
                val active = code == language
                Box(
                    Modifier.clip(RoundedCornerShape(9.dp)).background(if (active) Color.White else Color.Transparent)
                        .clickable { onLanguageChange(code) }.padding(horizontal = 9.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = if (active) Teal else Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun StatsRow(game: GameState, russian: Boolean) {
    val filled = game.values.indices.count { game.values[it] == game.solution[it] }
    val progress = (filled * 100f / 81f).roundToInt()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(if (russian) "ВРЕМЯ" else "TIME", formatTime(game.seconds), Modifier.weight(1f))
        StatCard(if (russian) "ПОПЫТКИ" else "TRIES", "${game.mistakes} / 3", Modifier.weight(1f))
        StatCard(if (russian) "ГОТОВО" else "DONE", "$progress%", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp)) {
            Text(label, color = Muted, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Text(value, color = TealDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DifficultySelector(selected: String, russian: Boolean, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F7F3)).padding(4.dp)) {
        listOf("easy" to if (russian) "Легко" else "Easy", "medium" to if (russian) "Средне" else "Medium", "hard" to if (russian) "Сложно" else "Hard").forEach { (key, label) ->
            val active = selected == key
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (active) Color.White else Color.Transparent)
                    .clickable { onSelect(key) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, fontSize = 11.sp, color = if (active) Teal else Muted, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SudokuBoard(game: GameState, hintCell: Int, onSelect: (Int) -> Unit) {
    Column(
        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(7.dp))
            .border(2.dp, Color(0xFF637B79), RoundedCornerShape(7.dp)).background(Color.White),
    ) {
        repeat(9) { row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                repeat(9) { col ->
                    val index = row * 9 + col
                    SudokuCell(game, index, hintCell, Modifier.weight(1f).fillMaxHeight()) { onSelect(index) }
                }
            }
        }
    }
}

@Composable
private fun SudokuCell(game: GameState, index: Int, hintCell: Int, modifier: Modifier, onClick: () -> Unit) {
    val value = game.values[index]
    val selectedValue = game.values[game.selected]
    val isGiven = game.puzzle[index] != 0
    val isSelected = index == game.selected
    val isWrong = value != 0 && value != game.solution[index]
    val bg = when {
        isSelected && isWrong -> Color(0xFFFFE4D9)
        isSelected -> Color(0xFFCDE8DD)
        isWrong -> Color(0xFFFFF0EA)
        selectedValue != 0 && value == selectedValue -> Color(0xFFE2F0E8)
        index != game.selected && SudokuEngine.arePeers(index, game.selected) -> Color(0xFFF2F7F3)
        else -> Color.White
    }
    Box(
        modifier.drawBehind {
            drawRect(bg)
            if (index % 9 != 8) {
                val major = index % 9 == 2 || index % 9 == 5
                drawLine(if (major) Color(0xFF77918D) else GridLine, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width, size.height), if (major) 2.dp.toPx() else 0.7.dp.toPx())
            }
            if (index / 9 != 8) {
                val major = index / 9 == 2 || index / 9 == 5
                drawLine(if (major) Color(0xFF77918D) else GridLine, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), if (major) 2.dp.toPx() else 0.7.dp.toPx())
            }
            if (isSelected) drawRect(Teal.copy(alpha = 0.9f), style = Stroke(width = 1.5.dp.toPx()))
            if (hintCell == index) drawCircle(Coral.copy(alpha = 0.22f), radius = size.minDimension * 0.46f)
        }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            value != 0 -> Text(value.toString(), color = when {
                isWrong -> Color(0xFFD56F56)
                isGiven -> Ink
                else -> Teal
            }, fontSize = 20.sp, fontWeight = if (isGiven) FontWeight.Bold else FontWeight.Medium)
            game.notes[index] != 0 -> Column(Modifier.fillMaxSize().padding(1.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                repeat(3) { noteRow ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        repeat(3) { noteCol ->
                            val note = noteRow * 3 + noteCol + 1
                            Text(if (game.notes[index] and (1 shl (note - 1)) != 0) note.toString() else "",
                                fontSize = 7.sp, color = Teal, fontWeight = FontWeight.Bold, lineHeight = 8.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberPad(game: GameState, onNumber: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        (1..9).forEach { number ->
            val used = game.values.indices.count { game.values[it] == number && game.solution[it] == number }
            val active = game.values[game.selected] == number
            Column(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(9.dp))
                    .background(if (active) Teal else Color(0xFFF5F8F4))
                    .clickable(enabled = used < 9 && game.status == "playing") { onNumber(number) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(number.toString(), color = if (active) Color.White else Teal, fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
                Text((9 - used).toString(), color = if (active) Color(0xFFCCE4DC) else Color(0xFF9CAB9E), fontSize = 8.sp, fontWeight = FontWeight.Bold, lineHeight = 9.sp)
            }
        }
    }
}

@Composable
private fun ProgressLine(game: GameState, russian: Boolean) {
    val filled = game.values.indices.count { game.values[it] == game.solution[it] }
    val progress = filled / 81f
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (russian) "Прогресс" else "Progress", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(9.dp))
        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Color(0xFFEAF1E9))) {
            Canvas(Modifier.fillMaxSize()) { drawRoundRect(Teal, size = size.copy(width = size.width * progress), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)) }
        }
        Spacer(Modifier.width(8.dp))
        Text("${(progress * 100).roundToInt()}%", color = Teal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToolsRow(game: GameState, russian: Boolean, onNotes: () -> Unit, onHint: () -> Unit, onErase: () -> Unit, onUndo: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        ToolButton(if (russian) "✎" else "✎", if (russian) "Заметки" else "Notes", game.noteMode, game.status == "playing", Modifier.weight(1f), onNotes)
        ToolButton("✦", if (russian) "Подсказка ${3 - game.hintsUsed}" else "Hint ${3 - game.hintsUsed}", false, game.status == "playing" && game.hintsUsed < 3, Modifier.weight(1f), onHint)
        ToolButton("⌫", if (russian) "Стереть" else "Erase", false, game.status == "playing", Modifier.weight(1f), onErase)
        ToolButton("↶", if (russian) "Отменить" else "Undo", false, game.status == "playing", Modifier.weight(1f), onUndo)
    }
}

@Composable
private fun ToolButton(symbol: String, label: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(60.dp).clip(RoundedCornerShape(13.dp))
            .background(if (selected) PaleGreen else Color.White)
            .border(1.dp, if (selected) Color(0xFFB8D8C9) else Color(0xFFF0F2ED), RoundedCornerShape(13.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 3.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(symbol, color = if (enabled) Teal else Muted.copy(alpha = 0.45f), fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
        Text(label, color = if (enabled) Color(0xFF53736F) else Muted.copy(alpha = 0.55f), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun createGame(level: Int, difficulty: String): GameState {
    val (solution, puzzle) = SudokuEngine.newPuzzle(difficulty)
    val selected = puzzle.indices.firstOrNull { puzzle[it] == 0 } ?: 0
    return GameState(level = level, difficulty = difficulty, solution = solution, puzzle = puzzle,
        values = puzzle.copyOf(), selected = selected)
}

private fun formatTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

private fun saveGame(preferences: android.content.SharedPreferences, game: GameState) {
    val json = JSONObject()
        .put("level", game.level)
        .put("difficulty", game.difficulty)
        .put("solution", JSONArray(game.solution.toList()))
        .put("puzzle", JSONArray(game.puzzle.toList()))
        .put("values", JSONArray(game.values.toList()))
        .put("notes", JSONArray(game.notes.toList()))
        .put("selected", game.selected)
        .put("mistakes", game.mistakes)
        .put("hintsUsed", game.hintsUsed)
        .put("seconds", game.seconds)
        .put("status", game.status)
    preferences.edit().putString("game", json.toString()).apply()
}

private fun loadGame(preferences: android.content.SharedPreferences): GameState? {
    return try {
        val json = preferences.getString("game", null)?.let { JSONObject(it) } ?: return null
        fun array(name: String): IntArray {
            val values = json.getJSONArray(name)
            require(values.length() == 81)
            return IntArray(81) { values.getInt(it) }
        }
        val solution = array("solution")
        val puzzle = array("puzzle")
        val values = array("values")
        val notes = array("notes")
        require(solution.all { it in 1..9 } && puzzle.all { it in 0..9 } && values.all { it in 0..9 } && notes.all { it in 0..511 })
        require(puzzle.indices.all { puzzle[it] == 0 || puzzle[it] == solution[it] })
        val difficulty = json.optString("difficulty", "easy")
        require(difficulty in setOf("easy", "medium", "hard"))
        GameState(
            level = json.optInt("level", 1).coerceIn(1, 999_999), difficulty = difficulty,
            solution = solution, puzzle = puzzle, values = values, notes = notes,
            selected = json.optInt("selected", 40).coerceIn(0, 80),
            mistakes = json.optInt("mistakes", 0).coerceIn(0, 3),
            hintsUsed = json.optInt("hintsUsed", 0).coerceIn(0, 3),
            seconds = json.optInt("seconds", 0).coerceIn(0, 359_999),
            status = json.optString("status", "playing").takeIf { it in setOf("playing", "won", "lost") } ?: "playing",
        )
    } catch (_: Exception) {
        null
    }
}
