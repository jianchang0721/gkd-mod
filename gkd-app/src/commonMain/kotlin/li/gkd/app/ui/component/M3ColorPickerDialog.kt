package li.gkd.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import li.gkd.app.resources.Res
import li.gkd.app.resources.action_cancel
import li.gkd.app.resources.action_confirm
import li.gkd.app.resources.m3_color_brightness
import li.gkd.app.resources.m3_color_contrast
import li.gkd.app.resources.m3_color_hex
import li.gkd.app.resources.m3_color_hue
import li.gkd.app.resources.m3_color_primary
import li.gkd.app.resources.m3_color_saturation
import li.gkd.app.resources.m3_color_variant
import li.gkd.app.resources.palette_content
import li.gkd.app.resources.palette_expressive
import li.gkd.app.resources.palette_fidelity
import li.gkd.app.resources.palette_fruit_salad
import li.gkd.app.resources.palette_monochrome
import li.gkd.app.resources.palette_neutral
import li.gkd.app.resources.palette_rainbow
import li.gkd.app.resources.palette_tonal_spot
import li.gkd.app.resources.palette_vibrant
import li.gkd.app.ui.style.HsvColor
import li.gkd.app.ui.style.PaletteStyleOption
import li.gkd.app.ui.style.argbToHsv
import li.gkd.app.ui.style.formatHexColor
import li.gkd.app.ui.style.hsvToArgb
import li.gkd.app.ui.style.parseHexColor
import org.jetbrains.compose.resources.stringResource

/** M3 配色设置: 主色(色相/饱和度/明度/HEX) + 配色方案 + 对比度. */
@Composable
fun M3ColorPickerDialog(
    seedArgb: Long,
    variant: PaletteStyleOption,
    contrastLevel: Double,
    onDismiss: () -> Unit,
    onConfirm: (seedArgb: Long, variant: PaletteStyleOption, contrastLevel: Double) -> Unit,
) {
    val initial = remember(seedArgb) { argbToHsv(seedArgb) }
    var hue by rememberSaveable { mutableFloatStateOf(initial.hue) }
    var saturation by rememberSaveable { mutableFloatStateOf(initial.saturation) }
    var brightness by rememberSaveable { mutableFloatStateOf(initial.value) }
    var selectedVariant by rememberSaveable { mutableStateOf(variant) }
    var contrast by rememberSaveable { mutableFloatStateOf(contrastLevel.toFloat()) }
    val currentArgb = hsvToArgb(HsvColor(hue, saturation, brightness))
    var hexInput by rememberSaveable { mutableStateOf(formatHexColor(seedArgb)) }
    LaunchedEffect(currentArgb) { hexInput = formatHexColor(currentArgb) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.m3_color_primary)) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(currentArgb)),
                )
                Text(
                    text = stringResource(Res.string.m3_color_hex),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { text ->
                        hexInput = text
                        parseHexColor(text)?.let { argb ->
                            val hsv = argbToHsv(argb)
                            hue = hsv.hue
                            saturation = hsv.saturation
                            brightness = hsv.value
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                )
                SliderRow(
                    label = stringResource(Res.string.m3_color_hue),
                    value = hue,
                    valueRange = 0f..360f,
                    onValueChange = { hue = it },
                )
                SliderRow(
                    label = stringResource(Res.string.m3_color_saturation),
                    value = saturation,
                    valueRange = 0f..1f,
                    onValueChange = { saturation = it },
                )
                SliderRow(
                    label = stringResource(Res.string.m3_color_brightness),
                    value = brightness,
                    valueRange = 0f..1f,
                    onValueChange = { brightness = it },
                )
                SliderRow(
                    label = stringResource(Res.string.m3_color_contrast),
                    value = contrast,
                    valueRange = -1f..1f,
                    onValueChange = { contrast = it },
                )
                Text(
                    text = stringResource(Res.string.m3_color_variant),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PaletteStyleOption.entries.forEach { option ->
                        FilterChip(
                            selected = selectedVariant == option,
                            onClick = { selectedVariant = option },
                            label = { Text(paletteStyleLabel(option)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(currentArgb, selectedVariant, contrast.toDouble())
                },
            ) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(text = value.toString().take(5), style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value.coerceIn(valueRange),
            valueRange = valueRange,
            onValueChange = onValueChange,
        )
    }
}

@Composable
private fun paletteStyleLabel(option: PaletteStyleOption): String = when (option) {
    PaletteStyleOption.TonalSpot -> stringResource(Res.string.palette_tonal_spot)
    PaletteStyleOption.Neutral -> stringResource(Res.string.palette_neutral)
    PaletteStyleOption.Vibrant -> stringResource(Res.string.palette_vibrant)
    PaletteStyleOption.Expressive -> stringResource(Res.string.palette_expressive)
    PaletteStyleOption.Rainbow -> stringResource(Res.string.palette_rainbow)
    PaletteStyleOption.FruitSalad -> stringResource(Res.string.palette_fruit_salad)
    PaletteStyleOption.Monochrome -> stringResource(Res.string.palette_monochrome)
    PaletteStyleOption.Fidelity -> stringResource(Res.string.palette_fidelity)
    PaletteStyleOption.Content -> stringResource(Res.string.palette_content)
}
