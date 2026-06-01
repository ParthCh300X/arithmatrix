package udemy.appdev.arithmatrix.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HearingDisabled
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import udemy.appdev.arithmatrix.ui.BottomNavBar
import udemy.appdev.arithmatrix.viewmodel.VoiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    navController: NavHostController,
    viewModel: VoiceViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val expression by viewModel.expression.collectAsState()
    val result by viewModel.result.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()

    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasMicPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasMicPerm = ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPerm = granted
        if (granted) viewModel.startListening()
    }

    val pulseScale = if (isListening) {
        rememberInfiniteTransition(label = "micPulse")
            .animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                label = "scale"
            ).value
    } else 1f

    Scaffold(
        topBar = { VoiceTopBar(navController) },
        bottomBar = { BottomNavBar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Expression", style = MaterialTheme.typography.titleMedium)
                Text(
                    expression.ifEmpty { "Say something like: '12 plus 5 times 3'" },
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(16.dp))
                Text("Result", style = MaterialTheme.typography.titleMedium)
                Text(result.ifEmpty { "—" }, style = MaterialTheme.typography.headlineMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(onClick = { viewModel.toggleTts() }) {
                    Icon(
                        imageVector = if (ttsEnabled) Icons.Default.Hearing else Icons.Default.HearingDisabled,
                        contentDescription = "Toggle TTS"
                    )
                }
                FloatingActionButton(
                    onClick = {
                        if (!hasMicPerm) {
                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            if (isListening) viewModel.stopListening() else viewModel.startListening()
                        }
                    },
                    modifier = Modifier.size(88.dp * pulseScale)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = "Mic")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTopBar(navController: NavHostController) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "ArithMatrix - Voice Mode",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        actions = {
            // Now routes to unified history, not voiceHistory
            IconButton(onClick = { navController.navigate("history") }) {
                Icon(Icons.Default.History, contentDescription = "History")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}