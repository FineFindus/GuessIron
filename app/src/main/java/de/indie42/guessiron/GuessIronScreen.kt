
package de.indie42.guessiron

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GuessIronScreen(
    onClickShowMeasured: () -> Unit,
    onClickEditScala: () -> Unit,
    onShowDisclaimer: () -> Unit,
    viewModel: GuessIronViewModel = viewModel(),
) {

    val guessIronState by viewModel.uiState.collectAsState()
    val measuredValues by viewModel.dataState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val scope = rememberCoroutineScope()

    var showSaveDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    if (!measuredValues.disclaimerDisabled){
        onShowDisclaimer()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { }
    ) {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var switchTopForCenter = 0F
                    detectDragGestures(onDragStart = { offset ->
                        switchTopForCenter = offset.y
                    }) { _, dragAmount ->
                        viewModel.increaseMeasuredPixel(dragAmount.y, switchTopForCenter)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset -> viewModel.updateMeasuredPixel(offset.y) }
                },
            color = MaterialTheme.colorScheme.background
        ) {
            ScalaBar(
                direction = guessIronState.scalaDirection,
                scalaPosition = ScalaPosition.Left,
                measureOffset = Offset(0F, guessIronState.measuredPixel),
                scalaFactor = measuredValues.scalaFactor
            )
            ScalaBar(
                direction = guessIronState.scalaDirection,
                scalaPosition = ScalaPosition.Right,
                measureOffset = Offset(0F, guessIronState.measuredPixel),
                scalaFactor = measuredValues.scalaFactor
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = {}) {

                    if (guessIronState.scalaDirection == ScalaDirection.Center)
                    {
                        val centerValue = guessIronState.measuredDistance / 2
                        Text(text = AnnotatedString(text = "$centerValue | ${guessIronState.measuredDistance} ${guessIronState.measuredUnit}"))
                    }
                    else
                        Text(text = AnnotatedString(text = "${guessIronState.measuredDistance} ${guessIronState.measuredUnit}"))
                }

                NavigationMenu(
                    scalaDirection = guessIronState.scalaDirection,
                    onClickSwitchScala = {
                        scope.launch {
                            viewModel.switchScala()
                        }
                    },
                    onClickSaveMeasuredValue = {
                        showSaveDialog = true
                    },
                    onClickNaviIcon = onClickShowMeasured,
                    onStartCalibration = onClickEditScala
                )
            }
        }
    }

    SnackbarMeasured(snackbarHostState, onClickShowMeasured)

    if (showSaveDialog) {
        val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val savedTimestamp = LocalDateTime.now().format(pattern)

        SaveDialog(
            measuredValue = guessIronState.measuredDistance,
            measuredUnit = guessIronState.measuredUnit,
            defaultName = "",
            onDismissRequest = { showSaveDialog = false }) {

            val newMeasuredItem =
                MeasuredValue.newBuilder()
                    .setTimestamp(savedTimestamp)
                    .setMeasured(guessIronState.measuredDistance)
                    .setName(it)

            scope.launch {
                viewModel.addMeasuredValue(measuredValue = newMeasuredItem.build())
                snackbarHostState.showSnackbar( message = "", duration = SnackbarDuration.Short )
            }
            showSaveDialog = false
        }
    }
}

@Composable
private fun SnackbarMeasured(
    snackbarHostState: SnackbarHostState,
    onClickShowMeasured: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), Alignment.BottomCenter) {
        SnackbarHost(hostState = snackbarHostState) {
            Snackbar(modifier = Modifier.padding(
                start = 16.dp, end = 16.dp
            ),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                action = {
                    TextButton(onClick = onClickShowMeasured) {
                        Text(text = stringResource(id = R.string.Show))
                    }
                }) {
                Text(text = stringResource(id = R.string.Measured_Value_Saved))
            }
        }
    }
}
