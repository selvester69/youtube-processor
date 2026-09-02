# Free Local YouTube & Shorts AI Hashtag Analyzer

A **100% free, privacy-first local desktop application** built in Java (Spring Boot) that connects to your YouTube account, analyzes your videos and Shorts, and uses a **locally hosted AI engine (Ollama + Llama 3)** to generate high-reach viral hashtags.

---

## 🌟 System Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        LOCAL DESKTOP SYSTEM                            │
│                                                                        │
│  ┌──────────────────┐    OAuth 2.0 / REST    ┌──────────────────────┐  │
│  │                  │ ────────────────────> │  YouTube Data API v3 │  │
│  │                  │ <──────────────────── │  (Google Cloud)      │  │
│  │                  │                       └──────────────────────┘  │
│  │   Java Desktop   │                                                  │
│  │   Application    │    HTTP REST POST     ┌──────────────────────┐  │
│  │  (Spring Boot +  │ ────────────────────> │   Ollama Engine      │  │
│  │   Web Dashboard) │ <──────────────────── │  (Model: llama3)     │  │
│  │                  │                       └──────────────────────┘  │
│  │                  │                                                  │
│  │                  │    JDBC / SQL         ┌──────────────────────┐  │
│  │                  │ ────────────────────> │   H2 Database        │  │
│  │                  │ <──────────────────── │   (Local Disk File)  │  │
│  └──────────────────┘                       └──────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Key Features

- **100% Free ($0 Cost)**: Uses official free YouTube Data API quota (10,000 units/day) and open-source local AI (Ollama).
- **YouTube Shorts & Videos**: Automatically detects standard videos vs. vertical YouTube Shorts (9:16 aspect ratio / under 60 seconds).
- **Local AI Hashtag Generation**: Connects to Ollama (`llama3` or `mistral`) running locally on `http://localhost:11434`.
- **One-Click YouTube Update**: Generates viral hashtags and automatically appends them to your YouTube video descriptions via OAuth 2.0.
- **Embedded Local Database**: Stores video history, views, likes, and generated hashtags in an embedded H2 database.
- **Web Dashboard**: Modern, dark-mode browser UI served directly by Spring Boot at `http://localhost:8080`.

---

## 🛠️ Step-by-Step Installation & Setup Guide

### 1. Install Local Software Prerequisites

#### **A. Java 17+ and Maven 3.8+**
Ensure OpenJDK 17 or higher and Apache Maven are installed on your computer.
- **Linux / Ubuntu**: `sudo apt update && sudo apt install openjdk-17-jdk maven`
- **macOS**: `brew install openjdk@17 maven`
- **Windows**: Download JDK 17+ and Maven from official sites or use `winget install Microsoft.OpenJDK.17`.

Verify installation:
```bash
java -version
mvn -version
```

#### **B. Install Ollama (Local AI Engine)**
Ollama allows you to run open-source LLMs locally on your computer for free.
- **Linux**: `curl -fsSL https://ollama.com/install.sh | sh`
- **macOS**: `brew install ollama` (or download from [ollama.com](https://ollama.com/download))
- **Windows**: Download installer from [ollama.com/download](https://ollama.com/download)

Pull the **llama3** or **mistral** model:
```bash
ollama pull llama3
```

Start the Ollama server (runs on `http://localhost:11434`):
```bash
ollama serve
```

---

### 2. Configure Google YouTube Data API v3 Credentials

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project named `Local YouTube Analyzer`.
3. Go to **APIs & Services > Library**, search for **YouTube Data API v3**, and click **Enable**.
4. Go to **APIs & Services > OAuth consent screen**:
   - Choose **External** (or Internal for Workspace).
   - Set App Name and Support Email.
   - Add scope: `https://www.googleapis.com/auth/youtube.force-ssl` (allows reading videos and updating descriptions).
   - Add your Google account under **Test Users**.
5. Go to **APIs & Services > Credentials > Create Credentials > OAuth client ID**:
   - Application type: **Desktop App**
   - Name: `YouTube Local App`
6. Click **Download JSON** and save the file as `client_secret.json` in the root folder of this project (`/client_secret.json`).

> **Note**: If `client_secret.json` is not present, the app operates in **Demo Mode**, serving sample videos and demonstrating local AI hashtag generation without connecting to live YouTube channels.

---

## 📦 Building and Running the Application

### 1. Build the Project
```bash
mvn clean package
```

### 2. Run the Application
```bash
mvn spring-boot:run
```
Or run the packaged JAR:
```bash
java -jar target/youtube-hashtags-app-1.0.0.jar
```

---

## 💻 Using the Application

1. Open your browser and navigate to **`http://localhost:8080`**.
2. **First-Time OAuth Authentication**:
   - When you click "Analyze & Update YouTube" for the first time, a browser window will pop up prompting you to log in to your Google Account.
   - Upon authorization, a refresh token will be saved locally under `./tokens/`.
   - Subsequent requests will authenticate headlessly without prompting you again!
3. **Generate Hashtags**:
   - Click **Generate AI Hashtags** to run local Ollama analysis on any video or short.
   - Click **Analyze & Update YouTube** to push the generated hashtags directly to your YouTube video description.

---

## 🧪 Running Unit & Integration Tests

To run all automated test suites:
```bash
mvn clean test
```

---

## 📄 License
MIT License. Free for personal and commercial use.
