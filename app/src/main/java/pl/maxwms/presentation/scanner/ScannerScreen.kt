package pl.maxwms.presentation.scanner

import android.Manifest
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import pl.maxwms.presentation.theme.GlossyButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import pl.maxwms.R
import pl.maxwms.presentation.components.StockAdjustSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onEditPart: (String) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var granted by remember { mutableStateOf(context.hasCameraPermission()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val snackbar = remember { SnackbarHostState() }
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }
    var flash by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(if (flash) 0.55f else 0f, label = "scanFlash")
    val missingText = state.missingCode?.let { stringResource(R.string.scanner_not_found, it) }
    DisposableEffect(tone) { onDispose { tone.release() } }
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ScanEffect.Found -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                    flash = true
                    delay(140)
                    flash = false
                }
                ScanEffect.Missing -> haptic.performHapticFeedback(HapticFeedbackType.Reject)
                ScanEffect.Adjusted -> haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        }
    }
    LaunchedEffect(missingText) {
        if (missingText != null) {
            snackbar.showSnackbar(missingText)
            viewModel.clearMissing()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scanner_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.45f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color.Black,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !context.hasCameraHardware() -> Message(stringResource(R.string.scanner_no_camera))
                !granted -> PermissionRequest { launcher.launch(Manifest.permission.CAMERA) }
                else -> {
                    BarcodeCamera(onBarcode = viewModel::onBarcode, modifier = Modifier.fillMaxSize())
                    Text(
                        text = stringResource(R.string.scanner_hint),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha)),
            )
        }
    }
    val part = state.part
    if (part != null) {
        StockAdjustSheet(
            part = part,
            message = state.insufficient?.let { stringResource(R.string.scanner_insufficient, it) },
            busy = state.adjusting,
            onDismiss = viewModel::dismiss,
            onAdjust = viewModel::adjust,
            onEdit = {
                viewModel.dismiss()
                onEditPart(part.id)
            },
        )
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.scanner_permission_title), style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(stringResource(R.string.scanner_permission_body), style = MaterialTheme.typography.bodyLarge, color = Color.White)
        GlossyButton(onClick = onRequest, modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Text(stringResource(R.string.scanner_permission_button))
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(24.dp))
    }
}
