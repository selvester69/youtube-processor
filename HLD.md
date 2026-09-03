# High-Level Design Document (HLD)
## Free Local YouTube & Shorts AI Hashtag Analyzer

---

## 1. Executive Summary & Production Readiness Assessment

The **Free Local YouTube & Shorts AI Hashtag Analyzer** is a privacy-first local desktop application designed for zero-cost YouTube content analysis, viral hashtag generation, and automated metadata updating.

### Production Readiness Analysis:
- **Cost**: **$0 / month**. Leverages free YouTube Data API v3 quota (10,000 quota units/day) and open-source local LLMs via Ollama.
- **Security & Privacy**: Zero third-party cloud AI dependency. Video metadata is processed strictly on the user's local machine via Ollama on `http://localhost:11434`.
- **Fault Tolerance**: Graceful fallback modes for both YouTube OAuth (`client_secret.json`) and local AI Ollama connectivity.
- **Extensibility**: Architected with SOLID principles, Strategy Pattern, and Adapter Pattern to allow seamless addition of future social platforms (e.g. Instagram Reels, Facebook Videos).

---

## 2. System Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          LOCAL DESKTOP SYSTEM                           │
│                                                                         │
│  ┌────────────────────────┐  OAuth 2.0 / REST  ┌──────────────────────┐ │
│  │                        │ ─────────────────> │  YouTube Data API v3 │ │
│  │                        │ <───────────────── │  (Google Cloud)      │ │
│  │                        │                    └──────────────────────┘ │
│  │   Java Desktop App     │                                             │
│  │  (Spring Boot 3.2.2)   │   HTTP REST POST   ┌──────────────────────┐ │
│  │                        │ ─────────────────> │   Ollama Engine      │ │
│  │  ┌──────────────────┐  │ <───────────────── │  (Model: llama3)     │ │
│  │  │ Platform Registry│  │                    └──────────────────────┘ │
│  │  └────────┬─────────┘  │                                             │
│  │           │            │    JDBC / SQL      ┌──────────────────────┐ │
│  │  ┌────────▼─────────┐  │ ─────────────────> │   H2 Database        │ │
│  │  │ YouTube Adapter  │  │ <───────────────── │   (Local Disk File)  │ │
│  │  └──────────────────┘  │                    └──────────────────────┘ │
│  └────────────────────────┘                                             │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. SOLID Principles & Design Patterns Application

The application core is restructured around **SOLID principles** and battle-tested software engineering design patterns:

### **Single Responsibility Principle (SRP)**
- `YouTubeService`: Handles low-level Google Client library calls, OAuth 2.0 code flow, and YouTube API endpoint execution.
- `OllamaService`: Manages REST payloads and communication with the local Ollama HTTP endpoint.
- `YouTubePlatformAdapter`: Adapts YouTube specific operations to the generic `SocialPlatformAdapter` interface.
- `WorkflowService`: Coordinates data sync, AI generation, and persistence logic.

### **Open/Closed Principle (OCP)**
- The core platform interface (`SocialPlatformAdapter`) and dynamic registry (`SocialPlatformRegistry`) allow developers to plug in new platform adapters (e.g. Instagram, Facebook, TikTok) by implementing `SocialPlatformAdapter` and annotating with `@Component`. No existing service code needs modification.

### **Liskov Substitution Principle (LSP)**
- Any `SocialPlatformAdapter` implementation can be substituted interchangeably in `SocialPlatformRegistry` without breaking callers in `WorkflowService`.

### **Interface Segregation Principle (ISP)**
- `SocialPlatformAdapter` defines a clean, cohesive contract specifically tailored for fetching content, generating hashtags, updating metadata, and sharing videos.

### **Dependency Inversion Principle (DIP)**
- `WorkflowService` depends on high-level abstractions (`SocialPlatformRegistry` and `SocialPlatformAdapter`) rather than concrete platform client implementations.

---

## 4. Class Structure & Design Patterns

```
              ┌───────────────────────────┐
              │  <<SocialPlatformAdapter>>│
              └─────────────┬─────────────┘
                            │
              ┌─────────────┴─────────────┐
              │   YouTubePlatformAdapter  │
              └─────────────┬─────────────┘
                            │
              ┌─────────────▼─────────────┐
              │   SocialPlatformRegistry  │
              └─────────────┬─────────────┘
                            │
              ┌─────────────▼─────────────┐
              │      WorkflowService      │
              └───────────────────────────┘
```

### Key Design Patterns:
1. **Adapter Pattern**: `YouTubePlatformAdapter` adapts the YouTube Data API SDK into a unified `SocialPlatformAdapter` interface.
2. **Strategy Pattern**: Different platform strategies can be selected dynamically at runtime based on platform key (`YOUTUBE`, etc.).
3. **Registry Pattern**: `SocialPlatformRegistry` acts as a centralized locator and manager for all registered platform adapters.

---

## 5. Extensibility Model: Adding New Platforms

To add a new social platform (e.g., Instagram Reels or Facebook Videos):

1. **Create Adapter**:
   ```java
   @Component
   public class InstagramPlatformAdapter implements SocialPlatformAdapter {
       @Override public String getPlatformType() { return "INSTAGRAM"; }
       @Override public boolean isConfigured() { return true; }
       @Override public List<VideoItem> fetchContent() { ... }
       @Override public boolean updateDescription(String contentId, String hashtags) { ... }
       @Override public boolean shareContent(VideoItem video, String targetPlatform) { ... }
   }
   ```
2. **Auto-Discovery**:
   Spring Boot automatically injects all beans implementing `SocialPlatformAdapter` into `SocialPlatformRegistry`.
3. **Instant API Availability**:
   The registry exposes the new platform adapter to `WorkflowService` and `AppController` without any code changes in business logic.

---

## 6. Execution Flow Sequence

1. **Content Fetching**: `AppController` -> `WorkflowService` -> `SocialPlatformRegistry.getAdapter("YOUTUBE")` -> `YouTubePlatformAdapter.fetchContent()`.
2. **AI Hashtag Generation**: `WorkflowService` -> `OllamaService.generateHashtags(title, description, isShort)` -> Local Ollama endpoint `http://localhost:11434`.
3. **Database Sync**: Generated tags and metadata persisted in local H2 database (`./data/ytdb`).
4. **Platform Update**: `WorkflowService` -> `SocialPlatformAdapter.updateDescription()` -> `YouTubeService` -> YouTube Data API v3 update call.

---

## 7. Data Storage Schema (H2 Local Database)

### Table: `VIDEOS`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `VIDEO_ID` | VARCHAR(255) | PRIMARY KEY | Unique YouTube video or Short ID |
| `TITLE` | VARCHAR(255) | NOT NULL | Title of video |
| `DESCRIPTION` | VARCHAR(2000)| NULLABLE | Content description |
| `THUMBNAIL_URL` | VARCHAR(255) | NULLABLE | Thumbnail image URL |
| `IS_SHORT` | BOOLEAN | NOT NULL | Flag for YouTube Shorts (under 60s) |
| `VIEW_COUNT` | BIGINT | NULLABLE | Total view count |
| `LIKE_COUNT` | BIGINT | NULLABLE | Total like count |
| `GENERATED_HASHTAGS` | VARCHAR(1000)| NULLABLE | Ollama generated hashtags |
| `UPDATED_ON_YOUTUBE` | BOOLEAN | NOT NULL | Sync status to YouTube |
| `LAST_ANALYZED_AT` | TIMESTAMP | NULLABLE | Last timestamp analyzed by AI |
