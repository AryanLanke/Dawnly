package com.dawnly.app.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyTheme

/** A text field made of glass: a soft label above, a rounded translucent pane, and a hint inside. */
@Composable
fun GlassField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String? = null,
) {
    val colors = DawnlyTheme.colors
    Column {
        SectionLabel(label)
        GlassSurface(Modifier.fillMaxWidth(), shape = GlassShapes.Button) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { field ->
                    Box(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        if (value.isEmpty()) {
                            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.textSoft.copy(alpha = 0.6f))
                        }
                        field()
                    }
                },
            )
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = colors.tick, modifier = Modifier.padding(start = 8.dp, top = 6.dp))
        }
    }
}
