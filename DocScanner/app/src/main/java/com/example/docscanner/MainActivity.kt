package com.example.docscanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { App() } } }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val records = remember { mutableStateListOf<Record>().also { it.addAll(Store.load(ctx)) } }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var hasScan by remember { mutableStateOf(false) }
    var recipient by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var outgoing by remember { mutableStateOf("") }
    var raw by remember { mutableStateOf("") }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun process(uri: Uri) {
        busy = true
        status = "Reading text… (first run downloads Arabic/English data)"
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) { Ocr.recognize(ctx, uri) }
                val f = Parser.parse(text)
                recipient = f.recipient; date = f.date; quantity = f.quantity; outgoing = f.outgoing
                raw = text
                hasScan = true
                status = if (text.isBlank()) "No text found. Try a sharper, well-lit photo." else "Check the fields, then Save."
            } catch (e: Exception) {
                status = "Error: ${e.message}"
            } finally {
                busy = false
            }
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { process(it) }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) process(uri)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Doc Scanner", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !busy, onClick = {
                val f = File(ctx.cacheDir, "images").apply { mkdirs() }.let { File(it, "scan_${System.currentTimeMillis()}.jpg") }
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
                cameraUri = uri
                camera.launch(uri)
            }) { Text("Camera") }
            Button(enabled = !busy, onClick = { picker.launch("image/*") }) { Text("Upload image") }
            OutlinedButton(enabled = records.isNotEmpty(), onClick = { exportAndShare(ctx, records) }) {
                Text("Export Excel (${records.size})")
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)

        if (hasScan) {
            OutlinedTextField(recipient, { recipient = it }, label = { Text("Intended recipient / المرسل إليه") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(date, { date = it }, label = { Text("Date / التاريخ") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity / الكمية") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(outgoing, { outgoing = it }, label = { Text("Outgoing / الصادر") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(raw, { raw = it }, label = { Text("Full text / النص الكامل") }, modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    records.add(Record(recipient, date, quantity, outgoing, raw))
                    Store.save(ctx, records)
                    hasScan = false; status = "Saved."
                }) { Text("Save") }
                TextButton(onClick = { hasScan = false; status = "" }) { Text("Discard") }
            }
        }

        HorizontalDivider()
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(records.reversed()) { _, r ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(r.recipient.ifBlank { "(no recipient)" }, style = MaterialTheme.typography.titleSmall)
                    Text("${r.date}  •  Qty ${r.quantity}  •  Out ${r.outgoing}", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
            }
        }
    }
}

private fun exportAndShare(ctx: Context, records: List<Record>) {
    val file = File(File(ctx.cacheDir, "exports"), "documents.xlsx")
    Xlsx.write(file, records)
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(send, "Save or share Excel file"))
}
