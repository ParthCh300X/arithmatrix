package udemy.appdev.arithmatrix.ui.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import udemy.appdev.arithmatrix.ui.BottomNavBar
import udemy.appdev.arithmatrix.ui.theme.ButtonDark
import udemy.appdev.arithmatrix.ui.theme.ButtonMid

private val SciColor = Color(0xFF4A4580)

data class ButtonCell(
    val label: String,
    val input: String = label,
    val type: CellType = CellType.DIGIT
)

enum class CellType { DIGIT, OPERATOR, SPECIAL, SCI, EMPTY, EQUALS }

// ── Normal mode: 5 rows × 4 cols = 20 buttons ────────────────────────────────
// No parentheses. Clean standard calculator layout.
private val buttons4col = listOf(
    // row 1
    ButtonCell("AC",  type = CellType.SPECIAL),
    ButtonCell("%",   type = CellType.SPECIAL),
    ButtonCell("⌫",   type = CellType.SPECIAL),
    ButtonCell("÷",   type = CellType.OPERATOR),
    // row 2
    ButtonCell("7"),  ButtonCell("8"),  ButtonCell("9"),
    ButtonCell("×",   type = CellType.OPERATOR),
    // row 3
    ButtonCell("4"),  ButtonCell("5"),  ButtonCell("6"),
    ButtonCell("-",   type = CellType.OPERATOR),
    // row 4
    ButtonCell("1"),  ButtonCell("2"),  ButtonCell("3"),
    ButtonCell("+",   type = CellType.OPERATOR),
    // row 5
    ButtonCell("00"), ButtonCell("0"),  ButtonCell("."),
    ButtonCell("=",   type = CellType.EQUALS),
)

// ── Scientific mode: 6 rows × 5 cols = 30 buttons ────────────────────────────
// Includes all 20 normal buttons + 9 sci + parentheses + ^ = 30 exactly
private val buttons5col = listOf(
    // row 1 — sci functions
    ButtonCell("sin", type = CellType.SCI),
    ButtonCell("cos", type = CellType.SCI),
    ButtonCell("tan", type = CellType.SCI),
    ButtonCell("√",   type = CellType.SCI),
    ButtonCell("x²",  type = CellType.SCI),
    // row 2 — more sci
    ButtonCell("log", type = CellType.SCI),
    ButtonCell("ln",  type = CellType.SCI),
    ButtonCell("^",   type = CellType.SCI),
    ButtonCell("(",   type = CellType.SPECIAL),
    ButtonCell(")",   type = CellType.SPECIAL),
    // row 3 — AC row
    ButtonCell("AC",  type = CellType.SPECIAL),
    ButtonCell("%",   type = CellType.SPECIAL),
    ButtonCell("⌫",   type = CellType.SPECIAL),
    ButtonCell("÷",   type = CellType.OPERATOR),
    ButtonCell("×",   type = CellType.OPERATOR),
    // row 4
    ButtonCell("7"),  ButtonCell("8"),  ButtonCell("9"),
    ButtonCell("-",   type = CellType.OPERATOR),
    ButtonCell("+",   type = CellType.OPERATOR),
    // row 5
    ButtonCell("4"),  ButtonCell("5"),  ButtonCell("6"),
    ButtonCell("1"),  ButtonCell("2"),
    // row 6
    ButtonCell("3"),
    ButtonCell("00"), ButtonCell("0"),  ButtonCell("."),
    ButtonCell("=",   type = CellType.EQUALS),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicScreen(
    navController: NavHostController,
    viewModel: BasicViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val expression   by viewModel.expression.collectAsState()
    val result       by viewModel.result.collectAsState()
    val isScientific by viewModel.isScientific.collectAsState()
    val haptic  = LocalHapticFeedback.current
    val context = LocalContext.current

    val columns = if (isScientific) 5 else 4
    val buttons = if (isScientific) buttons5col else buttons4col
    val rows    = buttons.size / columns  // 5 or 6

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "ArithMatrix",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleScientific() }) {
                        Icon(
                            Icons.Default.Functions,
                            contentDescription = "Toggle scientific mode",
                            tint = if (isScientific) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = { BottomNavBar(navController = navController) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            val totalW   = maxWidth
            val totalH   = maxHeight
            val gridPad  = 12.dp
            val gap      = 10.dp

            // buttonSize derived purely from width — never changes with row count
            val buttonSize = (totalW - gridPad * 2 - gap * (columns - 1)) / columns

            // exact grid height — no guessing, no weight fighting
            val gridHeight = buttonSize * rows + gap * (rows - 1) + 8.dp

            // display gets everything above the grid
            val displayHeight = totalH - gridHeight

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {

                // ── Display ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(displayHeight)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val exprScroll = rememberScrollState()
                        Text(
                            text = expression,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.horizontalScroll(exprScroll)
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val resultFontSize = when {
                                result.length > 12 -> 24.sp
                                result.length > 8  -> 32.sp
                                result.length > 5  -> 40.sp
                                else               -> 48.sp
                            }
                            Text(
                                text = result,
                                fontSize = resultFontSize,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            if (result.isNotEmpty() && result != "Error") {
                                IconButton(onClick = {
                                    val intent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(
                                            android.content.Intent.EXTRA_TEXT,
                                            "$expression = $result"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(
                                        android.content.Intent.createChooser(intent, "Share result")
                                    )
                                }) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Share result",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Grid ─────────────────────────────────────────────
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                        .padding(horizontal = gridPad),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalArrangement   = Arrangement.spacedBy(gap),
                    userScrollEnabled     = false
                ) {
                    items(buttons) { cell ->
                        GridButton(
                            cell        = cell,
                            size        = buttonSize,
                            haptic      = haptic,
                            isBackspace = cell.label == "⌫",
                            onClick     = {
                                if (cell.type != CellType.EMPTY) {
                                    viewModel.onInput(cell.input)
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun GridButton(
    cell: ButtonCell,
    size: androidx.compose.ui.unit.Dp,
    haptic: HapticFeedback?,
    isBackspace: Boolean = false,
    onClick: () -> Unit
) {
    val background = when (cell.type) {
        CellType.OPERATOR -> MaterialTheme.colorScheme.secondary
        CellType.SPECIAL  -> ButtonMid
        CellType.SCI      -> SciColor
        CellType.EQUALS   -> MaterialTheme.colorScheme.primary
        CellType.DIGIT    -> ButtonDark
        CellType.EMPTY    -> Color.Transparent
    }

    val description = when (cell.label) {
        "⌫"  -> "backspace"
        "√"  -> "square root"
        "x²" -> "x squared"
        "^"  -> "power"
        "÷"  -> "divide"
        "×"  -> "multiply"
        "+"  -> "plus"
        "-"  -> "minus"
        "="  -> "equals"
        "%"  -> "percent"
        else -> cell.label
    }

    if (cell.type == CellType.EMPTY) {
        Box(modifier = Modifier.size(size).aspectRatio(1f))
        return
    }

    Button(
        onClick = {
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape  = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = background),
        modifier = Modifier
            .size(size)
            .aspectRatio(1f)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (isBackspace) {
            Icon(
                Icons.Default.Backspace,
                contentDescription = "backspace",
                tint = Color.White,
                modifier = Modifier.size(size * 0.38f)
            )
        } else {
            Text(
                text = cell.label,
                fontSize = when {
                    cell.label.length >= 3 -> (size.value * 0.22f).sp
                    cell.label.length == 2 -> (size.value * 0.28f).sp
                    else                   -> (size.value * 0.36f).sp
                },
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                maxLines   = 1,
                softWrap   = false,
                textAlign  = TextAlign.Center
            )
        }
    }
}