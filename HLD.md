# High-Level Design Document (HLD)
## Free Local YouTube, Facebook & Instagram AI Hashtag Analyzer

---

## 1. Executive Summary & Production Readiness Assessment

The **Free Local YouTube, Facebook & Instagram AI Hashtag Analyzer** is a privacy-first local desktop application designed for zero-cost multi-platform social video analysis, viral hashtag generation, and automated metadata updating across YouTube, Facebook Reels/Pages, and Instagram Reels.

### Production Readiness Analysis:
- **Cost**: **$0 / month**. Leverages free YouTube Data API v3 quota (10,000 quota units/day), Meta Graph API v19.0 (Facebook/Instagram), and open-source local LLMs via Ollama.
- **Security & Privacy**: Zero third-party cloud AI dependency. Video metadata is processed strictly on the user's local machine via Ollama on `http://localhost:11434`.
- **Fault Tolerance**: Graceful fallback modes for YouTube OAuth (`client_secret.json`), Facebook (`facebook_token.json`), Instagram (`instagram_token.json`), and local AI Ollama connectivity.
- **Extensibility**: Architected with SOLID principles, Strategy Pattern, and Adapter Pattern to allow seamless addition of social platforms (YouTube, Instagram Reels, Facebook Reels, TikTok).

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
│  │                        │  Meta Graph API    ┌──────────────────────┐ │
│  │   Java Desktop App     │ ─────────────────> │ Facebook Pages &     │ │
│  │  (Spring Boot 3.2.2)   │ <───────────────── │ Reels API v19.0      │ │
│  │                        │                    └──────────────────────┘ │
│  │                        │  Meta Graph API    ┌──────────────────────┐ │
│  │  ┌──────────────────┐  │ ─────────────────> │ Instagram Reels &    │ │
│  │  │ Platform Registry│  │ <───────────────── │ Media API v19.0      │ │
│  │  └────────┬─────────┘  │                    └──────────────────────┘ │
│  │           │            │                                             │
│  │  ┌────────┴─────────┐  │   HTTP REST POST   ┌──────────────────────┐ │
│  │  │ Multi-Platform   │  │ ─────────────────> │   Ollama Engine      │ │
│  │  │ Adapters (YT/FB/ │  │ <───────────────── │  (Model: llama3)     │ │
│  │  │ IG)              │  │                    └──────────────────────┘ │
│  │  └──────────────────┘  │                                             │
│  │                        │    JDBC / SQL      ┌──────────────────────┐ │
│  │                        │ ─────────────────> │   H2 Database        │ │
│  │                        │ <───────────────── │   (Local Disk File)  │ │
│  └────────────────────────┘                    └──────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. SOLID Principles & Multi-Platform Architecture

The application core is structured around **SOLID principles** and extensible design patterns:

### **Single Responsibility Principle (SRP)**
- `YouTubeService`: Manages Google client SDK calls, OAuth 2.0 code flow, and YouTube API calls.
- `FacebookService`: Manages Meta Graph API v19.0 endpoints (`/{page-id}/videos`, `/me/accounts`) and Page Access Tokens.
- `InstagramService`: Manages Meta Instagram Graph API v19.0 endpoints (`/{ig-user-id}/media`) and User Access Tokens.
- `OllamaService`: Manages REST payloads and prompt execution with the local Ollama HTTP endpoint.
- `YouTubePlatformAdapter`, `FacebookPlatformAdapter`, `InstagramPlatformAdapter`: Adapt platform-specific API implementations to the generic `SocialPlatformAdapter` contract.

### **Open/Closed Principle (OCP)**
- Platform interface (`SocialPlatformAdapter`) and dynamic registry (`SocialPlatformRegistry`) allow developers to plug in new platform adapters (e.g. TikTok, LinkedIn) by implementing `SocialPlatformAdapter` and annotating with `@Component`. No existing service code needs modification.

### **Liskov Substitution Principle (LSP)**
- Any `SocialPlatformAdapter` implementation (`YouTubePlatformAdapter`, `FacebookPlatformAdapter`, `InstagramPlatformAdapter`) can be substituted interchangeably in `SocialPlatformRegistry` without breaking callers in `WorkflowService`.

### **Interface Segregation Principle (ISP)**
- `SocialPlatformAdapter` defines a clean, cohesive contract specifically tailored for fetching content, generating hashtags, updating metadata, and sharing videos.

### **Dependency Inversion Principle (DIP)**
- `WorkflowService` depends on high-level abstractions (`SocialPlatformRegistry` and `SocialPlatformAdapter`) rather than concrete platform client implementations.

---

## 4. Class Structure & Dynamic Registry Pattern

```
                       ┌───────────────────────────┐
                       │  <<SocialPlatformAdapter>>│
                       └─────────────┬─────────────┘
                                     │
         ┌───────────────────────────┼───────────────────────────┐
         │                           │                           │
┌────────┴──────────────┐   ┌────────┴──────────────┐   ┌────────┴──────────────┐
│ YouTubePlatformAdapter│   │FacebookPlatformAdapter│   │InstagramPlatformAdapter│
└────────┬──────────────┘   └────────┬──────────────┘   └────────┬──────────────┘
         │                           │                           │
         └───────────────────────────┼───────────────────────────┘
                                     │
                       ┌─────────────▼─────────────┐
                       │   SocialPlatformRegistry  │
                       └─────────────┬─────────────┘
                                     │
                       ┌─────────────▼─────────────┐
                       │      WorkflowService      │
                       └───────────────────────────┘
```

---

## 5. Facebook & Instagram Meta Graph API Integration Details

### **Facebook Graph API Integration**
- **API Version**: Meta Graph API v19.0
- **Authentication**: Page Access Token (configured via `facebook_token.json` or `facebook.page.access.token` property).
- **Required Permissions**: `pages_show_list`, `pages_read_engagement`, `pages_manage_posts`.
- **Key Endpoints**:
  - GET `/{page-id}/videos?fields=id,title,description,picture,views`: Fetches Page videos and Reels.
  - POST `/{video-id}?description={hashtags}`: Updates video description/caption.

### **Instagram Graph API Integration**
- **API Version**: Meta Instagram Graph API v19.0
- **Authentication**: Long-Lived User Access Token linked to Instagram Professional/Creator account (`instagram_token.json` or `instagram.access.token` property).
- **Required Permissions**: `instagram_basic`, `instagram_content_publish`, `pages_show_list`.
- **Key Endpoints**:
  - GET `/{ig-user-id}/media?fields=id,caption,media_type,media_url,thumbnail_url,like_count`: Fetches IG Reels and media.
  - POST `/{ig-media-id}?caption={hashtags}`: Updates IG Reel/Media caption.

---

## 6. Execution Flow Sequence

1. **Content Fetching**: `AppController` -> `WorkflowService` -> `SocialPlatformRegistry.getAdapter(platform)` -> Adapter `fetchContent()`.
2. **AI Hashtag Generation**: `WorkflowService` -> `OllamaService.generateHashtags(title, description, isShort)` -> Local Ollama endpoint `http://localhost:11434`.
3. **Database Sync**: Generated tags and metadata persisted in local H2 database (`./data/ytdb`).
4. **Platform Update**: `WorkflowService` -> `SocialPlatformAdapter.updateDescription()` -> Target Platform Service -> API update call.

---

## 7. Data Storage Schema (H2 Local Database)

### Table: `VIDEOS`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `VIDEO_ID` | VARCHAR(255) | PRIMARY KEY | Unique video/media ID across platforms |
| `TITLE` | VARCHAR(255) | NOT NULL | Title of video or reel |
| `DESCRIPTION` | VARCHAR(2000)| NULLABLE | Content description or caption |
| `THUMBNAIL_URL` | VARCHAR(255) | NULLABLE | Thumbnail image URL |
| `IS_SHORT` | BOOLEAN | NOT NULL | Flag for vertical short-form content (Shorts/Reels) |
| `VIEW_COUNT` | BIGINT | NULLABLE | Total view count |
| `LIKE_COUNT` | BIGINT | NULLABLE | Total like count |
| `GENERATED_HASHTAGS` | VARCHAR(1000)| NULLABLE | Ollama generated hashtags |
| `UPDATED_ON_YOUTUBE` | BOOLEAN | NOT NULL | Sync status to target platform |
| `LAST_ANALYZED_AT` | TIMESTAMP | NULLABLE | Last timestamp analyzed by AI |
