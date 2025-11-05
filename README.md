# Firebase Function Call

This is an example project to demonstrate issue with [Firebase AI Live](https://firebase.google.com/docs/ai-logic/live-api) function calling.

## Project Configuration

### Prerequisites

1. **Firebase Project**: Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. **Gemini API**: Ensure the Gemini API is enabled for your Firebase project (API key is managed automatically via `google-services.json`)

### Setup Steps

1. **Download `google-services.json`**:
   - Go to Firebase Console → Project Settings → General
   - Under "Your apps" section, download `google-services.json` for your Android app
   - Place the file in `/app/google-services.json`

2. **Sync Gradle**: Open the project in Android Studio and sync Gradle files

3. **Grant Audio Permission**: The app will request microphone permission at runtime

## How to Test the Project

### Expected Behavior

This project demonstrates a **function calling issue** with the Firebase AI Live API where the AI doesn't continue after receiving a function response.

### Test Steps

1. **Start the app** on your Android device or emulator

2. **Tap the microphone button** (floating action button in bottom-right corner)
   - The button will turn **red** indicating the conversation is active
   - Watch the logs appear on screen

3. **Wait for AI greeting**
   - The AI should say: *"Hello! Ask me what one plus one is!"*
   - You'll see logs showing:
     - 🔧 Initializing LiveModel...
     - 🔌 Connecting to session...
     - ✅ Session connected
     - 🎤 Starting audio conversation...
     - 💬 Sending initial greeting...
     - ✅ Greeting sent

4. **Ask the AI: "What is one plus one?"**
   - The AI should call the `onePlusOne` function
   - You'll see logs showing:
     - 📞 Function called: onePlusOne
     - 🔢 Executing onePlusOne...
     - ✅ onePlusOne result: {"result":2}
     - 📤 Returning FunctionResponsePart

5. **🐛 ISSUE: AI doesn't continue**
   - After the function response is sent, the AI should respond with the answer
   - **However, the AI does not continue the conversation**
   - The function call completes successfully but no audio response is generated

### Expected vs Actual

**Expected**: AI should say something like "One plus one equals two" after receiving the function result

**Actual**: AI receives the function response but doesn't generate any audio output

## Project Structure

- `onePlusOneTool`: A simple function declaration that returns `{"result": 2}`
- `handleFunctionCall()`: Handler that executes the function and returns the response
- `startAudioToAudio()`: Initializes the Live API with audio modality and function calling
- **System Instruction**: Tells the AI to use the `onePlusOne` tool when asked about 1+1
- **Initial Message**: Prompts the AI to greet the user and ask them to ask about 1+1

## Key Implementation Details

- **Model**: `gemini-live-2.5-flash-preview`
- **Response Modality**: Audio
- **Function Call Handler**: Synchronous handler that logs each step
- **Audio Permission**: Required for `startAudioConversation()`

## Issue Reproduction

This project reliably reproduces the issue where:
1. Function calls are successfully made
2. Function responses are successfully returned
3. But the AI does not continue generating audio output after receiving the function response

The on-screen logs provide complete visibility into the function calling lifecycle.
