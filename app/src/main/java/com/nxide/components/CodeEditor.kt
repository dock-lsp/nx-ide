package com.nxide.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nxide.data.CodeLanguage
import com.nxide.ui.theme.*

@Composable
fun CodeEditor(
    activeFile: String,
    code: String,
    language: CodeLanguage,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lines = code.split("\n")

    Column(modifier = modifier.fillMaxSize().background(NxBgPrimary)) {
        // Tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(NxBgSecondary)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active tab
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(NxBgPrimary)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when {
                        activeFile.endsWith(".kt") -> "🟣"
                        activeFile.endsWith(".xml") -> "🔵"
                        else -> "🟢"
                    },
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    activeFile,
                    fontSize = 11.sp,
                    color = NxTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(NxGreen)
        )

        // Editor body
        Row(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Line numbers
            Column(
                modifier = Modifier
                    .background(NxBgSecondary)
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                lines.forEachIndexed { index, _ ->
                    Text(
                        "${index + 1}",
                        fontSize = 11.sp,
                        color = NxTextMuted,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 0.dp)
                    )
                }
            }

            VerticalDivider(color = NxBorder)

            // Code content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        appendHighlightedCode(code, language)
                    },
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                    color = NxTextPrimary
                )
            }
        }

        // Status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(NxBgTertiary)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Ln ${lines.size}", fontSize = 10.sp, color = NxTextMuted)
            Text(language.displayName, fontSize = 10.sp, color = NxTextMuted)
            Text("UTF-8", fontSize = 10.sp, color = NxTextMuted)
            Spacer(Modifier.weight(1f))
            Text("🤖 AI", fontSize = 10.sp, color = NxGreen)
        }
    }
}

private fun AnnotatedString.Builder.appendHighlightedCode(code: String, language: CodeLanguage) {
    val lines = code.split("\n")
    lines.forEachIndexed { lineIndex, line ->
        when (language) {
            CodeLanguage.KOTLIN -> appendKotlinLine(line)
            CodeLanguage.XML -> appendXmlLine(line)
            CodeLanguage.GRADLE -> appendGradleLine(line)
            else -> append(line)
        }
        if (lineIndex < lines.size - 1) append("\n")
    }
}

private fun AnnotatedString.Builder.appendKotlinLine(line: String) {
    val trimmed = line.trimStart()
    when {
        trimmed.startsWith("//") -> {
            pushStyle(SpanStyle(color = SynComment, fontStyle = FontStyle.Italic))
            append(line)
            pop()
        }
        trimmed.startsWith("/*") || trimmed.startsWith("*") -> {
            pushStyle(SpanStyle(color = SynComment, fontStyle = FontStyle.Italic))
            append(line)
            pop()
        }
        else -> {
            var i = 0
            while (i < line.length) {
                val ch = line[i]
                when {
                    ch == '"' -> {
                        val end = line.indexOf('"', i + 1)
                        if (end != -1) {
                            pushStyle(SpanStyle(color = SynString))
                            append(line.substring(i, end + 1))
                            pop()
                            i = end + 1
                        } else {
                            pushStyle(SpanStyle(color = SynString))
                            append(line.substring(i))
                            pop()
                            break
                        }
                    }
                    ch == '/' && i + 1 < line.length && line[i + 1] == '/' -> {
                        pushStyle(SpanStyle(color = SynComment, fontStyle = FontStyle.Italic))
                        append(line.substring(i))
                        pop()
                        break
                    }
                    ch == '@' -> {
                        val end = line.indexOfAny(charArrayOf(' ', '(', '\n'), i)
                        val annotation = if (end != -1) line.substring(i, end) else line.substring(i)
                        pushStyle(SpanStyle(color = SynAnnotation))
                        append(annotation)
                        pop()
                        i += annotation.length
                    }
                    ch.isDigit() -> {
                        var j = i
                        while (j < line.length && (line[j].isDigit() || line[j] == '.' || line[j] == 'f' || line[j] == 'L')) j++
                        pushStyle(SpanStyle(color = SynNumber))
                        append(line.substring(i, j))
                        pop()
                        i = j
                    }
                    ch.isLetter() || ch == '_' -> {
                        var j = i
                        while (j < line.length && (line[j].isLetterOrDigit() || line[j] == '_')) j++
                        val word = line.substring(i, j)
                        val color = when {
                            KEYWORDS.contains(word) -> SynKeyword
                            word.firstOrNull()?.isUpperCase() == true -> SynType
                            i + word.length < line.length && line.getOrNull(i + word.length) == '(' -> SynFunction
                            else -> NxTextPrimary
                        }
                        pushStyle(SpanStyle(color = color, fontWeight = if (color == SynKeyword) FontWeight.Medium else FontWeight.Normal))
                        append(word)
                        pop()
                        i = j
                    }
                    else -> {
                        append(ch)
                        i++
                    }
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendXmlLine(line: String) {
    var i = 0
    while (i < line.length) {
        val ch = line[i]
        when {
            ch == '<' && line.startsWith("!--", i + 1) -> {
                pushStyle(SpanStyle(color = SynComment, fontStyle = FontStyle.Italic))
                append(line.substring(i))
                pop()
                break
            }
            ch == '<' -> {
                append(ch)
                i++
                if (i < line.length && line[i] == '/') { append('/'); i++ }
                val tagEnd = line.indexOfAny(charArrayOf(' ', '>', '/', '\n'), i)
                if (tagEnd != -1) {
                    pushStyle(SpanStyle(color = SynTag))
                    append(line.substring(i, tagEnd))
                    pop()
                    i = tagEnd
                }
            }
            ch == '"' -> {
                val end = line.indexOf('"', i + 1)
                if (end != -1) {
                    pushStyle(SpanStyle(color = SynString))
                    append(line.substring(i, end + 1))
                    pop()
                    i = end + 1
                } else {
                    pushStyle(SpanStyle(color = SynString))
                    append(line.substring(i))
                    pop()
                    break
                }
            }
            ch.isLetter() -> {
                var j = i
                while (j < line.length && (line[j].isLetterOrDigit() || line[j] == ':' || line[j] == '_')) j++
                val word = line.substring(i, j)
                if (j < line.length && line[j] == '=') {
                    pushStyle(SpanStyle(color = SynAttr))
                    append(word)
                    pop()
                } else {
                    append(word)
                }
                i = j
            }
            else -> { append(ch); i++ }
        }
    }
}

private fun AnnotatedString.Builder.appendGradleLine(line: String) {
    val trimmed = line.trimStart()
    if (trimmed.startsWith("//")) {
        pushStyle(SpanStyle(color = SynComment, fontStyle = FontStyle.Italic))
        append(line)
        pop()
    } else {
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    val end = line.indexOf('"', i + 1)
                    if (end != -1) {
                        pushStyle(SpanStyle(color = SynString))
                        append(line.substring(i, end + 1))
                        pop()
                        i = end + 1
                    } else {
                        pushStyle(SpanStyle(color = SynString))
                        append(line.substring(i))
                        pop()
                        break
                    }
                }
                ch == '/' && i + 1 < line.length && line[i + 1] == '/' -> {
                    pushStyle(SpanStyle(color = SynComment, fontStyle = FontStyle.Italic))
                    append(line.substring(i))
                    pop()
                    break
                }
                ch.isDigit() -> {
                    var j = i
                    while (j < line.length && line[j].isDigit()) j++
                    pushStyle(SpanStyle(color = SynNumber))
                    append(line.substring(i, j))
                    pop()
                    i = j
                }
                ch.isLetter() || ch == '_' -> {
                    var j = i
                    while (j < line.length && (line[j].isLetterOrDigit() || line[j] == '_')) j++
                    val word = line.substring(i, j)
                    val color = if (GRADLE_KEYWORDS.contains(word)) SynKeyword else NxTextPrimary
                    pushStyle(SpanStyle(color = color))
                    append(word)
                    pop()
                    i = j
                }
                else -> { append(ch); i++ }
            }
        }
    }
}

private val KEYWORDS = setOf(
    "fun", "val", "var", "class", "object", "interface", "when", "if", "else", "for", "while",
    "return", "import", "package", "override", "private", "public", "internal", "protected",
    "abstract", "open", "data", "sealed", "enum", "annotation", "companion", "by", "lazy",
    "is", "as", "in", "typealias", "suspend", "try", "catch", "finally", "throw", "true",
    "false", "null", "this", "super", "constructor", "init", "lateinit", "const", "vararg",
    "out", "reified", "inline", "crossinline", "noinline", "expect", "actual", "get", "set"
)

private val GRADLE_KEYWORDS = setOf(
    "plugins", "id", "android", "defaultConfig", "buildTypes", "dependencies",
    "implementation", "testImplementation", "androidTestImplementation", "debugImplementation",
    "compileOptions", "kotlinOptions", "buildFeatures", "composeOptions", "packaging",
    "repositories", "google", "mavenCentral", "apply", "version", "pluginManagement",
    "dependencyResolution", "rootProject", "include", "namespace", "compileSdk", "minSdk",
    "targetSdk", "versionCode", "versionName", "isMinifyEnabled", "proguardFiles",
    "jvmTarget", "sourceCompatibility", "targetCompatibility", "useSupportLibrary",
    "excludes", "platform"
)
