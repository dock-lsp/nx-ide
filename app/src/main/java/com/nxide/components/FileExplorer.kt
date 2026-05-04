package com.nxide.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nxide.data.FileNode
import com.nxide.data.FileType
import com.nxide.ui.theme.*

@Composable
fun FileExplorer(
    projectName: String,
    files: List<FileNode>,
    activeFile: String,
    onFileClick: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(NxBgSecondary)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "📁 项目文件",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = NxTextSecondary,
                letterSpacing = 0.5.sp
            )
            Row {
                Text("+", fontSize = 16.sp, color = NxTextMuted, modifier = Modifier.padding(horizontal = 4.dp))
                Text("↻", fontSize = 14.sp, color = NxTextMuted, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }

        // Project name
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NxGreen.copy(alpha = 0.05f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                "📦 $projectName",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = NxGreen
            )
        }

        HorizontalDivider(color = NxBorder)

        // File tree
        LazyColumn(modifier = Modifier.weight(1f)) {
            files.forEach { node ->
                renderFileNode(node, 0, activeFile, onFileClick, onFolderClick)
            }
        }
    }
}

private fun LazyListScope.renderFileNode(
    node: FileNode,
    depth: Int,
    activeFile: String,
    onFileClick: (String) -> Unit,
    onFolderClick: (String) -> Unit
) {
    item(key = "${node.name}_$depth") {
        FileTreeItem(node, depth, activeFile, onFileClick, onFolderClick)
    }
    if (node.type == FileType.FOLDER && node.isExpanded) {
        node.children.forEachIndexed { _, child ->
            renderFileNode(child, depth + 1, activeFile, onFileClick, onFolderClick)
        }
    }
}

@Composable
private fun FileTreeItem(
    node: FileNode,
    depth: Int,
    activeFile: String,
    onFileClick: (String) -> Unit,
    onFolderClick: (String) -> Unit
) {
    val isActive = node.type == FileType.FILE && node.name == activeFile

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (node.type == FileType.FOLDER) onFolderClick(node.name)
                else onFileClick(node.name)
            }
            .background(if (isActive) NxGreen.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent)
            .padding(start = (12 + depth * 16).dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.type == FileType.FOLDER) {
            Text(
                if (node.isExpanded) "▼" else "▶",
                fontSize = 9.sp,
                color = NxTextMuted,
                modifier = Modifier.width(14.dp)
            )
        } else {
            Spacer(Modifier.width(14.dp))
        }

        Text(
            when {
                node.type == FileType.FOLDER && node.isExpanded -> "📂"
                node.type == FileType.FOLDER -> "📁"
                node.name.endsWith(".kt") -> "🟣"
                node.name.endsWith(".xml") -> "🔵"
                node.name.endsWith(".kts") -> "🟢"
                node.name.endsWith(".properties") -> "⚙️"
                else -> "📄"
            },
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 6.dp)
        )

        Text(
            node.name,
            fontSize = 13.sp,
            color = if (isActive) NxGreen else NxTextSecondary,
            fontWeight = if (node.type == FileType.FOLDER || isActive) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
