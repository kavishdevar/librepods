package me.kavishdevar.librepods.presentation.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme

data class RichText(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)

@Composable
fun richText(
    source: String,
): RichText {
    val icons = LocalIcons.current
    val inlineContent = mutableMapOf<String, InlineTextContent>()

    val text = buildAnnotatedString {
        var i = 0
        var id = 0

        while (i < source.length) {
            if (!source.startsWith("\\icon{", i)) {
                append(source[i])
                i++
                continue
            }

            val end = source.indexOf('}', i)
            if (end == -1) {
                append(source.substring(i))
                break
            }

            val parts = source.substring(i + 6, end).split(',', limit = 2)

            val name = parts[0].trim()
            val tint = parts.getOrNull(1)?.trim()

            val vector = icons.fromName(name)

            val resolvedTint = tint?.parseColor(MaterialTheme.colorScheme, LocalContentColor.current) ?: LocalContentColor.current

            if (vector != null) {
                val key = "icon$id"

                appendInlineContent(key)

                inlineContent[key] = InlineTextContent(
                    Placeholder(
                        width = 1.125.em,
                        height = 1.125.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(if ((icons is MaterialIcons && !icons.isAppleIcon(name) || icons is AppleIcons && icons.isMaterialIcon(name))) 1.25f else 1f)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = vector,
                            contentDescription = null,
                            tint = resolvedTint,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxHeight()
//                             .border(Dp.Hairline, Color.Red),
                        )
                    }
                }

                id++
            } else {
                append(source.substring(i, end + 1))
            }

            i = end + 1
        }
    }

    return RichText(
        text = text,
        inlineContent = inlineContent
    )
}

fun String.parseColor(colorScheme: ColorScheme, defaultColor: Color): Color {
    if (startsWith("#")) {
        return runCatching {
            Color(this.toColorInt())
        }.getOrElse {
            defaultColor
        }
    }

    return when(this) {
        "primary" -> colorScheme.primary
        "onPrimary" -> colorScheme.onPrimary
        "primaryContainer" -> colorScheme.primaryContainer
        "onPrimaryContainer" -> colorScheme.onPrimaryContainer
        "inversePrimary" -> colorScheme.inversePrimary
        "secondary" -> colorScheme.secondary
        "onSecondary" -> colorScheme.onSecondary
        "secondaryContainer" -> colorScheme.secondaryContainer
        "onSecondaryContainer" -> colorScheme.onSecondaryContainer
        "tertiary" -> colorScheme.tertiary
        "onTertiary" -> colorScheme.onTertiary
        "tertiaryContainer" -> colorScheme.tertiaryContainer
        "onTertiaryContainer" -> colorScheme.onTertiaryContainer
        "background" -> colorScheme.surface
        "onBackground" -> colorScheme.onBackground
        "surface" -> colorScheme.surface
        "onSurface" -> colorScheme.onSurface
        "surfaceVariant" -> colorScheme.surfaceVariant
        "onSurfaceVariant" -> colorScheme.onSurfaceVariant
        "surfaceTint" -> colorScheme.surfaceTint
        "inverseSurface" -> colorScheme.inverseSurface
        "inverseOnSurface" -> colorScheme.inverseOnSurface
        "error" -> colorScheme.error
        "onError" -> colorScheme.onError
        "errorContainer" -> colorScheme.errorContainer
        "onErrorContainer" -> colorScheme.onErrorContainer
        "outline" -> colorScheme.outline
        "outlineVariant" -> colorScheme.outlineVariant
        "scrim" -> colorScheme.scrim
        "surfaceBright" -> colorScheme.surfaceBright
        "surfaceDim" -> colorScheme.surfaceDim
        "surfaceContainer" -> colorScheme.surfaceContainer
        "surfaceContainerHigh" -> colorScheme.surfaceContainerHigh
        "surfaceContainerHighest" -> colorScheme.surfaceContainerHighest
        "surfaceContainerLow" -> colorScheme.surfaceContainerLow
        "surfaceContainerLowest" -> colorScheme.surfaceContainerLowest
        "primaryFixed" -> colorScheme.primaryFixed
        "primaryFixedDim" -> colorScheme.primaryFixedDim
        "onPrimaryFixed" -> colorScheme.onPrimaryFixed
        "onPrimaryFixedVariant" -> colorScheme.onPrimaryFixedVariant
        "secondaryFixed" -> colorScheme.secondaryFixed
        "secondaryFixedDim" -> colorScheme.secondaryFixedDim
        "onSecondaryFixed" -> colorScheme.onSecondaryFixed
        "onSecondaryFixedVariant" -> colorScheme.onSecondaryFixedVariant
        "tertiaryFixed" -> colorScheme.tertiaryFixed
        "tertiaryFixedDim" -> colorScheme.tertiaryFixedDim
        "onTertiaryFixed" -> colorScheme.onTertiaryFixed
        "onTertiaryFixedVariant" -> colorScheme.onTertiaryFixedVariant
        else -> defaultColor
    }
}

// TODO: create a composable for previewing
@Preview
@Composable
fun RichTextPreview() {
    val designSystem = remember { mutableStateOf(DesignSystem.Material) }
    val darkTheme = remember { mutableStateOf(true) }

    LibrePodsTheme(
        designSystem = designSystem.value,
        darkTheme = darkTheme.value
    ) {
        val iconMap = LocalIcons.current.IconMap

        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceContainer),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(modifier = Modifier.height(topPadding))

            val materialAppleIconTestText = richText(
                source = "Text \\icon{Bluetooth,onBackground} \\icon{LeftCircleFill,onBackground} \\icon{BoltCircle,onBackground} \\icon{BoltCircle,onBackground} \\icon{RightCircleFill,onBackground} \\icon{AirPodsPro3CaseFill,onBackground} \\icon{BoltCircle,onBackground}"
            )

            Text(
                text = materialAppleIconTestText.text,
                inlineContent = materialAppleIconTestText.inlineContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        darkTheme.value = !darkTheme.value
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (darkTheme.value) "Light" else "Dark")
                }

                Button(
                    onClick = {
                        designSystem.value =
                            if (designSystem.value == DesignSystem.Apple) DesignSystem.Material else DesignSystem.Apple
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (designSystem.value == DesignSystem.Apple) "Material" else "Apple")
                }
            }

            iconMap.keys.forEach {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )

                    LibrePodsTheme(
                        designSystem = DesignSystem.Material,
                        darkTheme = darkTheme.value
                    ) {
                        val richText = richText(
                            source = "Text \\icon{$it,primary}"
                        )

                        Text(
                            text = richText.text,
                            inlineContent = richText.inlineContent,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 24.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }


                    LibrePodsTheme(
                        designSystem = DesignSystem.Apple,
                        darkTheme = darkTheme.value
                    ) {
                        val richText = richText(
                            source = "Text \\icon{$it,primary}"
                        )

                        Text(
                            text = richText.text,
                            inlineContent = richText.inlineContent,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 24.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onBackground)
                )
            }

            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}
