package mpo.qrcodescanner.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mpo.qrcodescanner.data.ScanResult
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel()
) {
    val scans by viewModel.allScans.collectAsState(initial = emptyList())
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        TopAppBar(
            title = { Text("Scan History") },
            actions = {
                IconButton(onClick = { viewModel.deleteAll() }) {
                    Icon(Icons.Outlined.DeleteSweep, "Clear History")
                }
            }
        )

        // Scan List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(scans) { scan ->
                ScanHistoryItem(
                    scan = scan,
                    onDelete = { viewModel.delete(scan) },
                    onToggleFavorite = { viewModel.toggleFavorite(scan) },
                    onShare = { shareContent(context, scan) },
                    onAction = { handleScanAction(context, scan) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanHistoryItem(
    scan: ScanResult,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onAction: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAction
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scan.type,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = dateFormat.format(scan.timestamp),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = scan.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy button
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Scan Content", scan.content)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Content copied", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Outlined.ContentCopy, "Copy")
                }

                // Share button
                IconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, "Share")
                }

                // Favorite button
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (scan.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        "Toggle Favorite"
                    )
                }

                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, "Delete")
                }
            }
        }
    }
}

private fun shareContent(context: Context, scan: ScanResult) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, scan.content)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

private fun handleScanAction(context: Context, scan: ScanResult) {
    when (scan.type) {
        "URL" -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scan.content))
            context.startActivity(intent)
        }
        "Contact" -> {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.NAME, scan.content)
            }
            context.startActivity(intent)
        }
        "WiFi" -> {
            // Handle WiFi connection (requires system permissions)
            Toast.makeText(context, "WiFi connection requires system permissions", Toast.LENGTH_SHORT).show()
        }
        else -> {
            // Copy to clipboard for other types
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Scan Content", scan.content)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Content copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
} 