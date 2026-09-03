# Free Local YouTube, Facebook & Instagram AI Hashtag Analyzer

A **100% free, privacy-first local desktop application** built in Java (Spring Boot) that connects to your **YouTube, Facebook Page, and Instagram Professional** accounts, analyzes your videos, Shorts, and Reels, and uses a **locally hosted AI engine (Ollama + Llama 3)** to generate high-reach viral hashtags.

---

## 🌟 System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          LOCAL DESKTOP SYSTEM                           │
│                                                                         │
│  ┌────────────────────────┐  OAuth 2.0 / REST  ┌──────────────────────┐ │
│  │                        │ ─────────────────> │  YouTube Data API v3 │ │
│  │                        │ <───────────────── │  (Google Cloud)      │ │
│  │                        │                    └──────────────────────┘ │
│  │                        │  Meta Graph API    ┌──────────────────────┐ │
│  │   Java Desktop App     │ ─────────────────> │ Facebook Pages &     │ │
│  │  (Spring Boot +        │ <───────────────── │ Reels API v19.0      │ │
│  │   Web Dashboard)       │                    └──────────────────────┘ │
│  │                        │  Meta Graph API    ┌──────────────────────┐ │
│  │                        │ ─────────────────> │ Instagram Reels &    │ │
│  │                        │ <───────────────── │ Media API v19.0      │ │
│  │                        │                    └──────────────────────┘ │
│  │                        │                                             │
│  │                        │   HTTP REST POST   ┌──────────────────────┐ │
│  │                        │ ─────────────────> │   Ollama Engine      │ │
│  │                        │ <───────────────── │  (Model: llama3)     │ │
│  │                        │                    └──────────────────────┘ │
│  │                        │                                             │
│  │                        │    JDBC / SQL      ┌──────────────────────┐ │
│  │                        │ ─────────────────> │   H2 Database        │ │
│  │                        │ <───────────────── │   (Local Disk File)  │ │
│  └────────────────────────┘                    └──────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Key Features

- **100% Free ($0 Cost)**: Uses official YouTube Data API quota, Meta Graph API free tier, and open-source local AI (Ollama).
- **Multi-Platform Support**: Works seamlessly across **YouTube Shorts & Videos**, **Facebook Reels & Videos**, and **Instagram Reels**.
- **Local AI Hashtag Generation**: Connects to Ollama (`llama3` or `mistral`) running locally on `http://localhost:11434`.
- **One-Click Metadata Update**: Generates viral hashtags and automatically appends them to your video descriptions and reel captions via APIs.
- **Extensible Adapter Architecture**: Built with SOLID principles (`SocialPlatformAdapter`, `SocialPlatformRegistry`) allowing instant addition of new platforms.
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

### 2. Configure Social Platform API Credentials

#### **A. YouTube Data API v3 Setup**
1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project named `Local YouTube Analyzer`.
3. Enable **YouTube Data API v3**.
4. Configure **OAuth consent screen** and add scope: `https://www.googleapis.com/auth/youtube.force-ssl`.
5. Create **OAuth 2.0 Client ID (Desktop App)**, download JSON, and save as `client_secret.json` in the project root folder.

#### **B. Facebook Pages & Reels API v3 Setup**
1. Go to [Meta for Developers](https://developers.facebook.com/).
2. Create a Meta App with **Facebook Login for Business** or **Page Management** products.
3. Obtain a Page Access Token with permissions `pages_show_list`, `pages_read_engagement`, `pages_manage_posts`.
4. Save config as `facebook_token.json` or set `facebook.page.id` and `facebook.page.access.token` in `application.properties`.

#### **C. Instagram Reels & Media API Setup**
1. Link your Instagram Professional/Creator account to your Facebook Page.
2. Under Meta Developers Console, get an Instagram User Access Token with permissions `instagram_basic` and `instagram_content_publish`.
3. Save config as `instagram_token.json` or set `instagram.user.id` and `instagram.access.token` in `application.properties`.

> **Note**: If API token files are not present, the app automatically runs in **Demo Mode**, serving realistic sample video data across platforms for instant testing.

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
2. **View Multi-Platform Active Adapters**: Check active adapter status badges (`YOUTUBE`, `FACEBOOK`, `INSTAGRAM`) in the dashboard header.
3. **Generate Hashtags**:
   - Click **Generate AI Hashtags** to run local Ollama analysis on any video, short, or reel.
   - Click **Analyze & Update Platform** to push generated hashtags directly to YouTube descriptions, Facebook reel captions, or Instagram captions.

---

## 🧪 Running Unit & Integration Tests

To run all automated test suites across all adapters and services:
```bash
mvn clean test
```

---

## 📄 License
MIT License. Free for personal and commercial use.
