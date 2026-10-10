package com.example.a4scanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.a4scanner.data.ScanPage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagesScreen(
    pages: List<ScanPage>,
    onBack: () -> Unit,
    onEdit: (ScanPage) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    onExport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pages (${pages.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (pages.isNotEmpty()) {
                Button(
                    onClick = onExport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export PDF")
                }
            }
        }
    ) { padding ->
        if (pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No pages yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Go back and take a picture of the first page",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.width(24.dp)
                            )
                            AsyncImage(
                                model = File(page.displayPath()),
                                contentDescription = "Page ${index + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(width = 56.dp, height = 76.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = page.filter.label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Initial crop",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { if (index > 0) onMove(index, index - 1) },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
                            }
                            IconButton(
                                onClick = { if (index < pages.size - 1) onMove(index, index + 1) },
                                enabled = index < pages.size - 1
                            ) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
                            }
                            IconButton(onClick = { onEdit(page) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onDelete(page.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
