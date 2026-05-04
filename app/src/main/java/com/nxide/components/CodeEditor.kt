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
import com.nxide.data.AppSettings
import com.nxide.data.CodeLanguage
import com.nxide.data.SearchState
import com.nxide.ui.theme.*

@Composable
fun CodeEditor(
    activeFile: String,
    code: String,
    language: CodeLanguage,
    onCodeChange: (String) -> Unit,
    settings: AppSettings = AppSettings(),
    searchState: SearchState = SearchState(),
    modifier: Modifier = Modifier
) {
    val lines = code.split("\n")

    Column(modifier = modifier.fillMaxSize().background(NxBgPrimary)) {
        // Editor body
        Row(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Line numbers
            if (settings.showLineNumbers) {
                Column(
                    modifier = Modifier
                        .background(NxBgSecondary)
                        .padding(top = 12.dp, bottom = 12.dp)
                ) {
                    lines.forEachIndexed { index, _ ->
                        Text(
                            "${index + 1}",
                            fontSize = settings.fontSize.sp,
                            color = NxTextMuted,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = (settings.fontSize * 1.7).sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp)
                        )
                    }
                }

                VerticalDivider(color = NxBorder)
            }

            // Code content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        appendHighlightedCode(code, language, searchState, settings)
                    },
                    fontSize = settings.fontSize.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = (settings.fontSize * 1.7).sp,
                    color = NxTextPrimary
                )
            }
        }

        // Status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(NxBgTertiary)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Ln ${lines.size}", fontSize = 11.sp, color = NxTextMuted)
            Text(language.displayName, fontSize = 11.sp, color = NxTextMuted)
            Text("UTF-8", fontSize = 11.sp, color = NxTextMuted)
            Text("Tab: ${settings.tabSize}", fontSize = 11.sp, color = NxTextMuted)
            Spacer(Modifier.weight(1f))
            Text("🤖 AI 辅助", fontSize = 11.sp, color = NxGreen)
        }
    }
}

private fun AnnotatedString.Builder.appendHighlightedCode(
    code: String,
    language: CodeLanguage,
    searchState: SearchState,
    settings: AppSettings
) {
    val lines = code.split("\n")
    lines.forEachIndexed { lineIndex, line ->
        when (language) {
            CodeLanguage.KOTLIN -> appendKotlinLine(line, searchState, settings)
            CodeLanguage.XML -> appendXmlLine(line, searchState)
            CodeLanguage.GRADLE -> appendGradleLine(line, searchState)
            else -> appendWithSearchHighlight(line, searchState)
        }
        if (lineIndex < lines.size - 1) append("\n")
    }
}

private fun AnnotatedString.Builder.appendWithSearchHighlight(line: String, searchState: SearchState) {
    if (searchState.query.isEmpty() || !searchState.isVisible) {
        append(line)
        return
    }
    var start = 0
    val query = searchState.query
    while (start < line.length) {
        val idx = line.indexOf(query, start, ignoreCase = !searchState.isCaseSensitive)
        if (idx == -1) {
            append(line.substring(start))
            break
        }
        if (idx > start) append(line.substring(start, idx))
        pushStyle(SpanStyle(background = NxYellow.copy(alpha = 0.3f), color = NxTextPrimary))
        append(line.substring(idx, idx + query.length))
        pop()
        start = idx + query.length
    }
}

private fun AnnotatedString.Builder.appendKotlinLine(line: String, searchState: SearchState, settings: AppSettings) {
    val trimmed = line.trimStart()
    when {
        trimmed.startsWith("//") -> {
            pushStyle(SpanStyle(color = SynComment, fontStyle = FontStyle.Italic))
            appendWithSearchHighlight(line, searchState)
            pop()
        }
        trimmed.startsWith("/*") || trimmed.startsWith("*") -> {
            pushStyle(SpanStyle(color = SynComment, fontStyle = FontStyle.Italic))
            appendWithSearchHighlight(line, searchState)
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
                            appendWithSearchHighlight(line.substring(i, end + 1), searchState)
                            pop()
                            i = end + 1
                        } else {
                            pushStyle(SpanStyle(color = SynString))
                            appendWithSearchHighlight(line.substring(i), searchState)
                            pop()
                            break
                        }
                    }
                    ch == '\'' -> {
                        val end = line.indexOf('\'', i + 1)
                        if (end != -1) {
                            pushStyle(SpanStyle(color = SynString))
                            appendWithSearchHighlight(line.substring(i, end + 1), searchState)
                            pop()
                            i = end + 1
                        } else {
                            append(ch)
                            i++
                        }
                    }
                    ch == '/' && i + 1 < line.length && line[i + 1] == '/' -> {
                        pushStyle(SpanStyle(color = SynComment, fontStyle = FontStyle.Italic))
                        appendWithSearchHighlight(line.substring(i), searchState)
                        pop()
                        break
                    }
                    ch == '@' -> {
                        val end = line.indexOfAny(charArrayOf(' ', '(', '\n'), i)
                        val annotation = if (end != -1) line.substring(i, end) else line.substring(i)
                        pushStyle(SpanStyle(color = SynAnnotation))
                        appendWithSearchHighlight(annotation, searchState)
                        pop()
                        i += annotation.length
                    }
                    ch.isDigit() -> {
                        var j = i
                        while (j < line.length && (line[j].isDigit() || line[j] == '.' || line[j] == 'f' || line[j] == 'L')) j++
                        pushStyle(SpanStyle(color = SynNumber))
                        appendWithSearchHighlight(line.substring(i, j), searchState)
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
                        appendWithSearchHighlight(word, searchState)
                        pop()
                        i = j
                    }
                    ch in "[]{}()\\\\" -> {
                        pushStyle(SpanStyle(color = NxOrange))
                        append(ch.toString())
                        pop()
                        i++
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

private fun AnnotatedString.Builder.appendXmlLine(line: String, searchState: SearchState) {
    var i = 0
    while (i < line.length) {
        val ch = line[i]
        when {
            ch == '<' && line.startsWith("!--", i + 1) -> {
                pushStyle(SpanStyle(color = SynComment, fontStyle = FontStyle.Italic))
                appendWithSearchHighlight(line.substring(i), searchState)
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
                    appendWithSearchHighlight(line.substring(i, tagEnd), searchState)
                    pop()
                    i = tagEnd
                }
            }
            ch == '"' -> {
                val end = line.indexOf('"', i + 1)
                if (end != -1) {
                    pushStyle(SpanStyle(color = SynString))
                    appendWithSearchHighlight(line.substring(i, end + 1), searchState)
                    pop()
                    i = end + 1
                } else {
                    pushStyle(SpanStyle(color = SynString))
                    appendWithSearchHighlight(line.substring(i), searchState)
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
                    appendWithSearchHighlight(word, searchState)
                    pop()
                } else {
                    appendWithSearchHighlight(word, searchState)
                }
                i = j
            }
            else -> { append(ch); i++ }
        }
    }
}

private fun AnnotatedString.Builder.appendGradleLine(line: String, searchState: SearchState) {
    val trimmed = line.trimStart()
    if (trimmed.startsWith("//")) {
        pushStyle(SpanStyle(color = SynComment, fontStyle = FontStyle.Italic))
        appendWithSearchHighlight(line, searchState)
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
                        appendWithSearchHighlight(line.substring(i, end + 1), searchState)
                        pop()
                        i = end + 1
                    } else {
                        pushStyle(SpanStyle(color = SynString))
                        appendWithSearchHighlight(line.substring(i), searchState)
                        pop()
                        break
                    }
                }
                ch == '/' && i + 1 < line.length && line[i + 1] == '/' -> {
                    pushStyle(SpanStyle(color = SynComment, fontStyle = FontStyle.Italic))
                    appendWithSearchHighlight(line.substring(i), searchState)
                    pop()
                    break
                }
                ch.isDigit() -> {
                    var j = i
                    while (j < line.length && line[j].isDigit()) j++
                    pushStyle(SpanStyle(color = SynNumber))
                    appendWithSearchHighlight(line.substring(i, j), searchState)
                    pop()
                    i = j
                }
                ch.isLetter() || ch == '_' -> {
                    var j = i
                    while (j < line.length && (line[j].isLetterOrDigit() || line[j] == '_')) j++
                    val word = line.substring(i, j)
                    val color = if (GRADLE_KEYWORDS.contains(word)) SynKeyword else NxTextPrimary
                    pushStyle(SpanStyle(color = color))
                    appendWithSearchHighlight(word, searchState)
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
