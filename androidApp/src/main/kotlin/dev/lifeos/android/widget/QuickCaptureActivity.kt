package dev.lifeos.android.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dev.lifeos.android.LifeOsApp
import dev.lifeos.data.Dates
import dev.lifeos.data.Source
import dev.lifeos.theme.LifeOsTheme
import kotlinx.coroutines.launch

/** Ventanita para apuntar una nota (a la Bandeja) o una tarea sin abrir la app. */
class QuickCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isTask = intent.getStringExtra(EXTRA_KIND) == TASK
        val repo = (application as LifeOsApp).repository
        setContent {
            LifeOsTheme {
                var text by remember { mutableStateOf("") }
                var forToday by remember { mutableStateOf(true) }
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                fun save() {
                    val t = text.trim()
                    if (t.isEmpty()) return
                    lifecycleScope.launch {
                        if (isTask) repo.addTask(t, plannedFor = if (forToday) Dates.today() else null)
                        else repo.capture(t, Source.WIDGET)
                        finish()
                    }
                }
                // Fondo oscurecido: tocar fuera cierra.
                Box(
                    Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { finish() }
                        .imePadding(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                            .clickable(remember { MutableInteractionSource() }, null) {},
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (isTask) "Nueva tarea" else "Apuntar en la bandeja", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                text, { text = it },
                                placeholder = { Text(if (isTask) "Qué hay que hacer" else "Lo que se te pase por la cabeza") },
                                singleLine = !isTask,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { save() }),
                                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isTask) {
                                    Checkbox(forToday, { forToday = it })
                                    Text("Para hoy", style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { finish() }) { Text("Cancelar") }
                                Spacer(Modifier.width(4.dp))
                                TextButton(onClick = { save() }, enabled = text.isNotBlank()) { Text("Guardar") }
                            }
                        }
                    }
                }
            }
        }
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.argb(0x66, 0, 0, 0)))
    }

    companion object {
        const val EXTRA_KIND = "kind"
        const val NOTE = "note"
        const val TASK = "task"
    }
}
