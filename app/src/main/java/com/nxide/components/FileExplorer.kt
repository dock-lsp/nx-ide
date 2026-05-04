package com.nxide.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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
    onNewFile: (String, String) -> Unit,
    onNewFolder: (String, String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onRenameFile: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var targetFolder by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }
    var contextMenuTarget by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .width(260.dp)
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
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            targetFolder = "main"
                            showNewFileDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 16.sp, color = NxTextMuted)
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            targetFolder = "main"
                            showNewFolderDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📁", fontSize = 12.sp, color = NxTextMuted)
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text("↻", fontSize = 14.sp, color = NxTextMuted)
                }
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
                renderFileNode(
                    node, 0, activeFile, onFileClick, onFolderClick,
                    onContextMenu = { name -> contextMenuTarget = name },
                    onNewFileInFolder = { folder ->
                        targetFolder = folder
                        showNewFileDialog = true
                    },
                    onNewFolderInFolder = { folder ->
                        targetFolder = folder
                        showNewFolderDialog = true
                    }
                )
            }
        }
    }

    // Context menu popup
    contextMenuTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { contextMenuTarget = null },
            containerColor = NxBgSecondary,
            title = { Text("文件操作", fontSize = 14.sp, color = NxTextPrimary) },
            text = {
                Column {
                    ContextMenuItem("📝 重命名") {
                        contextMenuTarget = null
                        renameText = target
                        showRenameDialog = target
                    }
                    ContextMenuItem("🗑️ 删除") {
                        contextMenuTarget = null
                        showDeleteConfirm = target
                    }
                    ContextMenuItem("📄 新建文件") {
                        contextMenuTarget = null
                        targetFolder = target
                        showNewFileDialog = true
                    }
                    ContextMenuItem("📁 新建文件夹") {
                        contextMenuTarget = null
                        targetFolder = target
                        showNewFolderDialog = true
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { contextMenuTarget = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("取消", fontSize = 12.sp, color = NxTextMuted)
                }
            }
        )
    }

    // New file dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false; newFileName = "" },
            containerColor = NxBgSecondary,
            title = { Text("新建文件", fontSize = 14.sp, color = NxTextPrimary) },
            text = {
                Column {
                    Text("目标文件夹: $targetFolder", fontSize = 12.sp, color = NxTextMuted)
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 13.sp, color = NxTextPrimary),
                        cursorBrush = SolidColor(NxGreen),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NxBgInput)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (newFileName.isEmpty()) {
                                    Text("文件名 (如: NewFile.kt)", fontSize = 13.sp, color = NxTextMuted)
                                }
                                inner()
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NxGreen)
                        .clickable {
                            if (newFileName.isNotBlank()) {
                                onNewFile(targetFolder, newFileName.trim())
                                newFileName = ""
                                showNewFileDialog = false
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("创建", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NxBgPrimary)
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showNewFileDialog = false; newFileName = "" }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("取消", fontSize = 12.sp, color = NxTextMuted)
                }
            }
        )
    }

    // New folder dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false; newFileName = "" },
            containerColor = NxBgSecondary,
            title = { Text("新建文件夹", fontSize = 14.sp, color = NxTextPrimary) },
            text = {
                Column {
                    Text("目标文件夹: $targetFolder", fontSize = 12.sp, color = NxTextMuted)
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 13.sp, color = NxTextPrimary),
                        cursorBrush = SolidColor(NxGreen),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NxBgInput)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (newFileName.isEmpty()) {
                                    Text("文件夹名称", fontSize = 13.sp, color = NxTextMuted)
                                }
                                inner()
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NxGreen)
                        .clickable {
                            if (newFileName.isNotBlank()) {
                                onNewFolder(targetFolder, newFileName.trim())
                                newFileName = ""
                                showNewFolderDialog = false
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("创建", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NxBgPrimary)
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showNewFolderDialog = false; newFileName = "" }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("取消", fontSize = 12.sp, color = NxTextMuted)
                }
            }
        )
    }

    // Delete confirmation
    showDeleteConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = NxBgSecondary,
            title = { Text("确认删除", fontSize = 14.sp, color = NxTextPrimary) },
            text = { Text("确定要删除 \"$target\" 吗？此操作不可撤销。", fontSize = 13.sp, color = NxTextSecondary) },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NxRed)
                        .clickable {
                            onDeleteFile(target)
                            showDeleteConfirm = null
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("删除", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NxTextPrimary)
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showDeleteConfirm = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("取消", fontSize = 12.sp, color = NxTextMuted)
                }
            }
        )
    }

    // Rename dialog
    showRenameDialog?.let { target ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            containerColor = NxBgSecondary,
            title = { Text("重命名", fontSize = 14.sp, color = NxTextPrimary) },
            text = {
                BasicTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 13.sp, color = NxTextPrimary),
                    cursorBrush = SolidColor(NxGreen),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(NxBgInput)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            inner()
                        }
                    }
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NxGreen)
                        .clickable {
                            if (renameText.isNotBlank() && renameText != target) {
                                onRenameFile(target, renameText.trim())
                            }
                            showRenameDialog = null
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("确定", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NxBgPrimary)
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showRenameDialog = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("取消", fontSize = 12.sp, color = NxTextMuted)
                }
            }
        )
    }
}

@Composable
private fun ContextMenuItem(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 13.sp, color = NxTextPrimary)
    }
}

private fun LazyListScope.renderFileNode(
    node: FileNode,
    depth: Int,
    activeFile: String,
    onFileClick: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    onContextMenu: (String) -> Unit,
    onNewFileInFolder: (String) -> Unit,
    onNewFolderInFolder: (String) -> Unit
) {
    item(key = "${node.name}_$depth") {
        FileTreeItem(node, depth, activeFile, onFileClick, onFolderClick, onContextMenu)
    }
    if (node.type == FileType.FOLDER && node.isExpanded) {
        node.children.forEachIndexed { _, child ->
            renderFileNode(
                child, depth + 1, activeFile, onFileClick, onFolderClick,
                onContextMenu, onNewFileInFolder, onNewFolderInFolder
            )
        }
    }
}

@Composable
private fun FileTreeItem(
    node: FileNode,
    depth: Int,
    activeFile: String,
    onFileClick: (String) -> Unit,
    onFolderClick: (String) -> Unit,
    onContextMenu: (String) -> Unit
) {
    val isActive = node.type == FileType.FILE && node.name == activeFile

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (node.type == FileType.FOLDER) onFolderClick(node.name)
                else onFileClick(node.name)
            }
            .clickable(
                enabled = true,
                onClick = { },
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
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
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Context menu button (⋮)
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(3.dp))
                .clickable { onContextMenu(node.name) },
            contentAlignment = Alignment.Center
        ) {
            Text("⋮", fontSize = 12.sp, color = NxTextMuted)
        }
    }
}
