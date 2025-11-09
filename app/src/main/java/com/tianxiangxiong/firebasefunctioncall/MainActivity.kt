package com.tianxiangxiong.firebasefunctioncall

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import com.tianxiangxiong.firebasefunctioncall.ui.theme.FirebaseFunctionCallTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

const val GEMINI_LIVE_MODEL = "gemini-live-2.5-flash-preview"

// Define the onePlusOne tool
val onePlusOneTool = FunctionDeclaration(
    name = "onePlusOne",
    description = "Returns the result of 1 + 1",
    parameters = emptyMap(),
)

// Function to execute the onePlusOne calculation
fun executeOnePlusOne(): JsonObject {
    return JsonObject(mapOf("result" to JsonPrimitive(2)))
}

// Function call handler that executes tool functions
fun handleFunctionCall(
    functionCall: FunctionCallPart,
    addLog: (String) -> Unit,
): FunctionResponsePart {
    addLog("📞 Function called: ${functionCall.name}")
    Log.d("MainActivity", "Function called: ${functionCall.name}, args: ${functionCall.args}")
    val response = when (functionCall.name) {
        "onePlusOne" -> {
            addLog("🔢 Executing onePlusOne...")
            val result = executeOnePlusOne()
            addLog("✅ onePlusOne result: $result")
            Log.d("MainActivity", "onePlusOne result: $result")
            FunctionResponsePart("onePlusOne", result, functionCall.id)
        }
        else -> {
            addLog("❌ Unknown function: ${functionCall.name}")
            Log.e("MainActivity", "Unknown function: ${functionCall.name}")
            FunctionResponsePart(
                functionCall.name,
                JsonObject(mapOf("error" to JsonPrimitive("Unknown function"))),
                functionCall.id
            )
        }
    }
    addLog("📤 Returning FunctionResponsePart")
    return response
}

class MainActivity : ComponentActivity() {
    private var hasAudioPermission = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasAudioPermission.value = isGranted
        if (!isGranted) {
            Log.e("MainActivity", "Audio permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check audio permission
        hasAudioPermission.value = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission.value) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        enableEdgeToEdge()
        setContent {
            FirebaseFunctionCallTheme {
                MainScreen(
                    hasPermission = hasAudioPermission.value,
                )
            }
        }
    }
}

@OptIn(PublicPreviewAPI::class)
@Composable
fun MainScreen(hasPermission: Boolean, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    var logs by remember { mutableStateOf(listOf<String>()) }
    var isConversationActive by remember { mutableStateOf(false) }
    var currentSession by remember { mutableStateOf<com.google.firebase.ai.type.LiveSession?>(null) }

    fun addLog(message: String) {
        logs = logs + message
        Log.d("MainActivity", message)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!hasPermission) {
                        addLog("❌ Audio permission required")
                        return@FloatingActionButton
                    }
                    coroutineScope.launch {
                        try {
                            if (isConversationActive) {
                                // Stop the conversation
                                addLog("⏹️ Stopping audio conversation...")
                                currentSession?.stopAudioConversation()
                                currentSession = null
                                isConversationActive = false
                                addLog("✅ Audio conversation stopped")
                            } else {
                                // Clear logs and start new conversation
                                logs = emptyList()
                                addLog("🎙️ Starting audio conversation...")
                                val session = startAudioToAudio(::addLog)
                                currentSession = session
                                isConversationActive = true
                                addLog("✅ Audio conversation started!")
                            }
                        } catch (e: Exception) {
                            addLog("❌ Error: ${e.message}")
                            Log.e("MainActivity", "Error with audio conversation", e)
                            isConversationActive = false
                            currentSession = null
                        }
                    }
                },
                containerColor = if (isConversationActive) Color.Red else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isConversationActive) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isConversationActive) "Stop conversation" else "Start conversation",
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            if (logs.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(logs.size) { index ->
                        Text(
                            text = logs[index],
                            modifier = Modifier.padding(vertical = 4.dp),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@RequiresPermission(Manifest.permission.RECORD_AUDIO)
@OptIn(PublicPreviewAPI::class)
suspend fun startAudioToAudio(addLog: (String) -> Unit): LiveSession {
    addLog("🔧 Initializing LiveModel...")
    // Initialize the Gemini Developer API backend service
    // Create a `LiveModel` instance with the flash-live model (only model that supports the Live API)
    val model = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
        modelName = GEMINI_LIVE_MODEL,
        // Configure the model to respond with audio
        generationConfig = liveGenerationConfig {
            responseModality = ResponseModality.AUDIO
        },
        // Add the onePlusOne tool
        tools = listOf(Tool.functionDeclarations(listOf(onePlusOneTool))),
        // Add system instruction to use the tool
        systemInstruction = content {
            text("You have access to a onePlusOne function. When the user asks what 1 + 1 is, use the onePlusOne tool to get the answer.")
        },
    )

    addLog("🔌 Connecting to session...")
    val session = model.connect()
    addLog("✅ Session connected")

    // This is the recommended way.
    // However, you can create your own recorder and handle the stream.
    // Add function call handler to execute tool functions
    addLog("🎤 Starting audio conversation...")
    session.startAudioConversation(
        functionCallHandler = { functionCall ->
            handleFunctionCall(functionCall, addLog)
        },
    )

    // Send initial greeting message - AI speaks to user
    addLog("💬 Sending initial greeting...")
    session.send("Say 'Hello! Ask me what one plus one is!' and wait for user input.")
    addLog("✅ Greeting sent")

    return session
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FirebaseFunctionCallTheme {
        MainScreen(hasPermission = true)
    }
}
