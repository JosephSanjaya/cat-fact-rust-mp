package sjy.sample.cat.fact

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import sjy.sample.cat.fact.ffi.CatFactRepository
import sjy.sample.cat.fact.ui.theme.CatFactTheme

import androidx.compose.material3.ExperimentalMaterial3Api

class MainActivity : ComponentActivity() {
    private val repository = CatFactRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CatFactRepository.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            CatFactTheme {
                CatFactScreen(
                    onFetchFact = { fetchCatFact(it) }
                )
            }
        }
    }

    private fun fetchCatFact(onResult: (Result<String>) -> Unit) {
        lifecycleScope.launch {
            val result = repository.getRandomFact()
            onResult(
                result.map { it.fact }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatFactScreen(
    onFetchFact: ((Result<String>) -> Unit) -> Unit
) {
    var catFact by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("🐱 Cat Facts") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            // Cat fact display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator()
                        }
                        error != null -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "❌ Error",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = error ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        catFact != null -> {
                            Text(
                                text = catFact ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text(
                                text = "Tap the button below to fetch a random cat fact!",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Fetch button
            Button(
                onClick = {
                    isLoading = true
                    error = null
                    onFetchFact { result ->
                        isLoading = false
                        result.fold(
                            onSuccess = { fact ->
                                catFact = fact
                                error = null
                            },
                            onFailure = { throwable ->
                                error = throwable.message ?: "Unknown error"
                                catFact = null
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                Text(
                    text = if (isLoading) "Loading..." else "Get Random Cat Fact",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Info text
            Text(
                text = "Powered by Rust 🦀 via UniFFI",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CatFactScreenPreview() {
    CatFactTheme {
        CatFactScreen(
            onFetchFact = { callback ->
                callback(Result.success("Cats sleep 70% of their lives."))
            }
        )
    }
}