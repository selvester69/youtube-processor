# Advanced Review: Staff Engineer Deep Dive + Execution & Review Plan

## 1. Advanced Architecture Gaps in My Previous Response

### 1.1 Missing Patterns

| Pattern | Gap in My Design | Why It Matters |
|---|---|---|
| **CQRS** | Dashboard queries same DB that writes → potential contention under batch load | Read replicas not needed locally, but separating query model helps dashboard perf (denormalized view) |
| **Event Sourcing** | No audit trail of "when hashtags were pushed" or "token refreshed" | Compliance + debugging: why did FB update fail on Tuesday? |
| **Saga Pattern** | Multi-platform update (YT + FB + IG in one bulk action) has no rollback if FB succeeds, IG fails | Partial failure leaves inconsistent state across platforms |
| **Rate-limit Backpressure** | Circuit breaker stops calls, but no priority queue — low-priority batch analytics get same treatment as user-requested hashtag push | Need weighted queue |
| **Materialized Views** | No pre-aggregated stats (e.g., "total views across all videos", "hashtag performance trends") | Dashboard forced to compute aggregates on-read → slow on large video library |
| **Multitenancy** | Assumes single local user — but enterprise version (paid tier) might have teams | No tenant isolation in schema |
| **Data Retention** | No retention policy — indefinite disk growth as H2 accumulates videos | Local disk space = constraint |

### 1.2 Advanced Additions

**Add these to P0/P1:**

1. **Event Log Table** (audit trail)
   ```
   EVENT_LOG
    - id (UUID, PK)
    - event_type (ENUM: HASHTAGS_GENERATED, UPDATE_PUSHED, TOKEN_REFRESHED, API_FAILED)
    - platform
    - video_id (FK)
    - details (JSON: {old_value, new_value, error})
    - created_at
    - UNIQUE(video_id, event_type, created_at)  -- prevent re-entrancy
   ```

2. **Materialized View** (cache aggregate stats)
   ```
   PLATFORM_STATS (refreshed every 5min by scheduler)
    - platform
    - total_videos
    - total_views
    - avg_views_per_video
    - videos_pending_hashtags
    - last_refreshed_at
   ```

3. **Saga Orchestration** (`BulkUpdateSaga`)
   - Before pushing to any platform, validate all targets are reachable (health check).
   - Push in order: YT → FB → IG (highest quota risk → lowest).
   - If any fails, rollback previous updates (POST old description back).
   - Log outcome in EVENT_LOG with compensation details.

4. **Priority Queue** for jobs
   ```
   JOB_QUEUE
    - id
    - job_type (GENERATE_HASHTAGS | PUSH_UPDATE | SYNC_VIDEOS)
    - priority (HIGH=user-triggered, MEDIUM=bulk, LOW=background)
    - status
    - retry_count
   ```
   ThreadPool processes HIGH/MEDIUM first; MEDIUM waits for quota availability.

5. **Encryption + Secrets Rotation**
   - Use Spring Vault (optional) or AWS Secrets Manager if deployed.
   - For local desktop, use Jasypt with master key stored in OS keychain (not plaintext).
   - Token rotation scheduled quarterly (add `last_rotated_at` column).

6. **Disaster Recovery**
   - H2 file backup on every successful push (to `./backups/db-{timestamp}.mv.db`).
   - On startup, if current DB corrupted, auto-restore from latest backup.

---

## 2. Advanced Dashboard Design

### 2.1 Analytics Layer (New)
Add secondary read model for dashboard — denormalized stats table:

```
VIDEO_ANALYTICS_VIEW (materialized view, refreshed on-demand)
 - platform
 - period (TODAY, WEEK, MONTH)
 - avg_views, sum_views
 - avg_likes, hashtag_engagement_delta (views before/after hashtag push)
 - videos_by_status (pie: generated/pushed/pending)
```

Dashboard queries this instead of aggregating raw VIDEOS table on each load.

### 2.2 Advanced UI Features
- **Recommendation Card**: "5 videos with <100 views + no hashtags — boost with AI?" → one-click batch.
- **Performance Chart**: "Hashtag impact" — overlay views before/after hashtag push per video.
- **Smart Filtering**: Save filter presets (e.g., "Short-form underperformers").
- **Bulk Actions Undo**: "Last 10 pushes" with revert buttons (fetches from EVENT_LOG + posts old description).
- **Token Health Dashboard**: Separate panel showing token status, expiry countdown, auto-refresh logs.

---

## 3. Execution Plan (8-Week Sprint Structure)

### Sprint 1-2 (Weeks 1-2): Foundation & Data Integrity
**Goal**: Fix P0 correctness bugs

| Task | Owner | Effort | Success Criteria |
|---|---|---|---|
| Schema migration: add PLATFORM, PLATFORM_CONNECTIONS | Backend | 3 days | Schema validated, old data migrated |
| Implement token encryption (Jasypt) | Backend | 2 days | Existing tokens encrypted on startup, no plaintext in logs |
| Add EVENT_LOG table + audit interceptor | Backend | 2 days | All adapter calls logged, retrievable via `/api/events?video_id=X` |
| Unit tests: idempotency guard on hashtag push | Backend | 1 day | Test suite covers dup-push scenario |
| **Review gate** | Lead | 0.5 day | Code review, schema validation test, security scan (tokens no longer exposed) |

**Exit Criteria**: No plaintext tokens in DB/logs, schema supports multi-platform correctly.

---

### Sprint 3-4 (Weeks 3-4): Resilience & Token Lifecycle
**Goal**: Handle failures gracefully, prevent silent auth failures

| Task | Owner | Effort | Success Criteria |
|---|---|---|---|
| Add Resilience4j to all 4 services (YouTube, Facebook, Instagram, Ollama) | Backend | 4 days | Retry + circuit breaker telemetry exposed in `/actuator/health` |
| Implement TokenRefreshService scheduled job | Backend | 2 days | Tokens refreshed 7 days before expiry, expired tokens raise alert in logs |
| Add QuotaTrackerService for YouTube | Backend | 2 days | Call-count tracked, graceful block at 95% daily quota |
| Create Saga orchestrator for multi-platform bulk updates | Backend | 3 days | Test: if FB push fails, rollback YT update; all outcomes in EVENT_LOG |
| Add integration tests against Meta/YouTube API mocks (Mockito/WireMock) | QA | 2 days | Test retry backoff, circuit break, quota exhaust scenarios |
| **Review gate** | Lead | 0.5 day | Resilience tests pass, saga compensations validated, no hardcoded timeouts |

**Exit Criteria**: Token refresh runs weekly without errors, failed API calls auto-retry + circuit-break without user intervention.

---

### Sprint 5 (Week 5): Async Processing & Job Queue
**Goal**: Unblock UI during batch operations

| Task | Owner | Effort | Success Criteria |
|---|---|---|---|
| Implement JOB_QUEUE schema + JobService | Backend | 2 days | Jobs persisted, retriable via DB (no in-memory loss on restart) |
| Configure ThreadPoolTaskExecutor with priority queue | Backend | 1 day | HIGH priority processes before MEDIUM before LOW |
| Add job progress endpoint `/api/jobs/{id}/progress` | Backend | 1 day | Dashboard polls this, shows "Processing 4/20 videos..." |
| Refactor hashtag generation to async tasks | Backend | 2 days | User clicks "Generate" → returns jobId immediately, UI polls progress |
| Integration test: verify job queue order under load | QA | 1 day | 20 jobs: HIGH processed first, retries respect backoff |
| **Review gate** | Lead | 0.5 day | No blocking calls on main thread, job table schema reviewed |

**Exit Criteria**: Batch hashtag generation on 100 videos completes in background, UI responsive.

---

### Sprint 6 (Week 6): Dashboard MVP
**Goal**: Unified multi-platform video listing

| Task | Owner | Effort | Success Criteria |
|---|---|---|---|
| Design dashboard schema + materialized view (PLATFORM_STATS) | Backend | 1 day | Schema reviewed, query performance <200ms on 1000 videos |
| Implement DashboardController + DashboardService (parallel adapter calls) | Backend | 2 days | `GET /api/videos?platform=ALL&sort=views` returns 50 videos in <500ms (with cache) |
| Implement caching layer (Caffeine) with TTL | Backend | 1 day | Video list cached 5min, manually refresh button available |
| Build dashboard UI: table + filters + bulk action bar | Frontend | 3 days | Filter by platform/status/date, multi-select, disabled bulk actions if no selection |
| Add platform connection status cards | Frontend | 1 day | Shows "YouTube: Connected", "Facebook: Token expires 2024-12-15", "Instagram: Not configured" |
| **Review gate** | Lead/UX | 0.5 day | UX review (accessibility, mobile responsive), performance profiling |

**Exit Criteria**: Dashboard loads, displays videos from all connected platforms, filters work, bulk action buttons available.

---

### Sprint 7 (Week 7): Advanced Features & Analytics
**Goal**: Dashboard insights + undo/revert

| Task | Owner | Effort | Success Criteria |
|---|---|---|---|
| Implement materialized analytics view query builder | Backend | 2 days | Dashboard can show "hashtag ROI" (views before/after push) per video |
| Add undo/revert endpoint: `POST /api/videos/{id}/revert-hashtag` | Backend | 1 day | Reverts to pre-hashtag description, logged in EVENT_LOG |
| Add "smart recommendation" card logic (identify underperformers) | Backend | 1 day | Returns list of <100-view videos missing hashtags |
| Implement token health dashboard UI | Frontend | 1.5 days | Shows expiry countdown, refresh status, manual re-auth button |
| Add performance/analytics chart (hashtag impact over time) | Frontend | 2 days | Line chart: views pre/post hashtag push per video |
| **Review gate** | Lead | 0.5 day | Analytics query performance validated, chart accessibility (alt text, keyboard nav) |

**Exit Criteria**: Dashboard shows insights, users can revert failed updates, token health visible.

---

### Sprint 8 (Week 8): Testing, Docs, Hardening
**Goal**: Production-readiness

| Task | Owner | Effort | Success Criteria |
|---|---|---|---|
| Add e2e test suite: login (OAuth) → sync videos → generate hashtags → push → verify DB | QA | 2 days | Full flow tested against mock APIs, passes 100/100 |
| Load test: 1000 video dashboard load, 100 concurrent hashtag generations | QA | 1.5 days | <2s response time for dashboard, queue processes 100 jobs without deadlock |
| Security audit: token storage, SQL injection, XSS in dashboard | Security | 1 day | No plaintext secrets, parameterized queries, CSP headers set |
| Write/update HLD.md with new architecture (CQRS, Saga, Event Log) | Docs | 1 day | HLD reflects current state, diagrams updated |
| Create runbook: disaster recovery (restore from backup), token rotation procedure | Docs | 0.5 day | Operator can restore from backup in <5min, rotate tokens in <2min |
| Create troubleshooting guide: common failure modes (Ollama down, API rate-limited, token expired) | Docs | 0.5 day | Operator follows guide to diagnose 80% of issues |
| **Final review gate** | Lead + Tech Lead | 1 day | Security sign-off, performance sign-off, docs complete, deployment checklist |

**Exit Criteria**: All tests green, security scan 0 critical/high vulns, docs reviewed, ready for release.

---

## 4. Review Plan for Development Team

### 4.1 Code Review Checklist (per PR)

**P0: Correctness**
- [ ] No plaintext tokens in new code or logs
- [ ] SQL queries use parameterized statements (no string concat)
- [ ] Async calls don't block main thread
- [ ] Retry logic includes exponential backoff + jitter
- [ ] Event logged for every external API call (platform + timestamp + outcome)

**P1: Resilience**
- [ ] Circuit breaker wired for 3rd-party calls
- [ ] Fallback behavior defined (return cached, fail-safe, or user-facing error msg)
- [ ] Timeout set on all blocking calls (no >30s hangs)
- [ ] Thread pool sized appropriately (review ThreadPoolTaskExecutor config)

**P2: Testability**
- [ ] New service method has unit test (mocked dependencies)
- [ ] Integration test uses WireMock/Mockito for external APIs
- [ ] Happy path + 3 failure scenarios tested per adapter

**P3: Observability**
- [ ] Structured logs (not string concatenation) with context (video_id, platform, user_id)
- [ ] Metrics registered (histogram for API latency, counter for retries)
- [ ] No PII in logs (no full tokens, no email addresses)

**P4: Performance**
- [ ] DB queries have EXPLAIN PLAN reviewed
- [ ] No N+1 queries (fetching 100 videos shouldn't hit API 100x)
- [ ] Caching strategy documented (TTL, invalidation)

---

### 4.2 Testing Strategy Matrix

| Level | What | Tools | Pass Criteria |
|---|---|---|---|
| **Unit** | Individual service methods (TokenRefreshService, OllamaService) | JUnit5 + Mockito | 85%+ coverage, all branches |
| **Integration** | Service → Repository → H2 DB, no mocks | @DataJpaTest + TestH2 | CRUD ops work, constraints enforced |
| **Contract** | Adapter → API mock (YouTube, Meta), ensure request/response shape correct | WireMock + Consumer-driven contract tests | Adapter handles API v19.0 correctly |
| **E2E** | Full flow: login → sync → generate → push → verify | Selenium/Playwright for UI, real DB | Happy path + 2 unhappy paths pass |
| **Load** | Dashboard with 1000 videos, 100 concurrent hashtag jobs | JMeter or Gatling | p99 latency <2s, no deadlocks, queue stable |
| **Chaos** | Ollama crashes mid-batch, FB token expires, YouTube quota exhausted | Inject failures via Resilience4j test utils | Circuit breaks, jobs retry, no data loss |

---

### 4.3 Design Review Checklist (before coding)

**For each new feature:**
- [ ] Does it follow SOLID? Which principle is at risk?
- [ ] Is there a failure mode? How do we handle it?
- [ ] Can it scale to 10k videos? (Does it require N API calls or 1 batch call?)
- [ ] Is there a security risk? (Token exposure, SQL injection, XSS, CORS?)
- [ ] What's the observability? (Can ops debug it without source code?)
- [ ] Is it testable without mocking externals? (Or do we have contract tests?)

---

### 4.4 Release Checklist (before shipping)

**Pre-release**
- [ ] All sprints' acceptance criteria met
- [ ] E2E tests pass against staging
- [ ] Security scan <5 low-severity issues (no critical/high)
- [ ] Performance benchmark: dashboard <2s, hashtag generation <5min/100 videos
- [ ] Runbook tested: disaster recovery, token rotation, troubleshooting guide

**Release**
- [ ] Rollback plan documented (restore DB from backup)
- [ ] Monitoring alerts configured (Ollama down, token refresh failed, queue stuck)
- [ ] User comms ready (what's new, known issues)

**Post-release (Day 1-7)**
- [ ] Monitor error logs for new failure modes
- [ ] Gather user feedback on dashboard UX
- [ ] Patch any high-severity bugs within 24h

---

## 5. Risk Register & Mitigation

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Token refresh job fails silently, users discover after 2 months | Medium | Critical | (Sprint 3) Refresh 7 days early, log every attempt, alert on failure |
| Bulk hashtag push partially fails (YT succeeds, FB fails) | Medium | High | (Sprint 4) Saga pattern with rollback, all outcomes logged |
| Dashboard becomes slow as video library grows (10k+ videos) | Medium | High | (Sprint 6) Implement pagination + materialized views, cache stats |
| Ollama crashes mid-batch, loses job progress | Low | Medium | (Sprint 5) Persist jobs in DB, retry on restart |
| User accidentally reverts wrong video's hashtags | Low | Low | (Sprint 7) Undo button + 7-day revert history in EVENT_LOG |
| Meta API deprecates v19.0 mid-year | Low | Medium | (Design) Pluggable adapter interface handles version bump by new adapter class |

---

## 6. Success Metrics (Acceptance Criteria by Phase)

| Metric | Target | Validation |
|---|---|---|
| **P0**: No token in logs/DB | 100% | Security scan + log grep: `token`, `password` = 0 results |
| **P1**: API failures auto-recover | 95% retry success rate | Chaos test: simulate 100 YouTube API 500s, 95+ auto-retry successfully |
| **Dashboard**: Load time | <1s (50 videos), <2s (500 videos) | ApacheBench: `ab -n 100 http://localhost:8080/api/videos` |
| **Dashboard**: Engagement | 80% of users generate hashtags within 5 clicks | UX telemetry (if shipped) |
| **Async jobs**: No blocking UI | 100% non-blocking | Chrome DevTools: main thread never >100ms blocked during hashtag generation |
| **Test coverage**: Core services | 85%+ branch coverage | JaCoCo report |
| **MTBF** (mean time between failures): Operational stability for 30 days | <1 failure per day | Monitoring dashboard (if deployed) |

---

## 7. Agent Task Assignments (by Role)

### Backend Engineer (2-3 people)
- Sprints 1-2: Schema migration + encryption
- Sprints 3-4: Resilience4j + TokenRefreshService + Saga
- Sprints 5-6: Async queue + DashboardService
- Sprints 7-8: Analytics views + testing

### Frontend Engineer (1 person)
- Sprints 6-7: Dashboard UI, filters, bulk actions, charts, token status
- Sprints 8: Accessibility audit, responsive design

### QA Engineer (1 person)
- Sprints 3-4: Integration tests vs mock APIs
- Sprints 5-8: E2E, load, chaos testing, security audit

### DevOps / Tech Lead (0.5 person)
- Sprint 8: Monitoring setup, runbook, troubleshooting guide, disaster recovery validation

---

## 8. Dependencies & Blockers

```
Sprint 1 ──→ Sprint 2 (schema must exist before encryption)
Sprint 2 ──→ Sprint 3 (tokenize before refresh job added)
Sprint 3 ──→ Sprint 4 (resilience before saga)
Sprint 4 ──→ Sprint 5 (reliability must be solid before queuing)
Sprint 5 ──→ Sprint 6 (queue stable before dashboard polling)
Sprints 1-6 ──→ Sprint 7 (only build analytics once core is solid)
Sprints 1-7 ──→ Sprint 8 (testing + hardening last)
```

**Critical path**: Sprints 1 → 2 → 3 → 4 → 5 → 6 (no parallelization without forking teams).

---

## Summary: What Changes from Initial Plan

| My Initial Proposal | Advanced Add-On |
|---|---|
| Resilience4j + retry | **+ Saga orchestrator for multi-platform consistency** |
| Token refresh job | **+ Event log audit trail + token rotation + encryption** |
| Dashboard listing | **+ Materialized analytics views + performance charts + undo/revert** |
| Single job queue | **+ Priority queue (HIGH/MEDIUM/LOW) + quota backpressure** |
| Basic caching | **+ Disaster recovery backups + schema versioning** |

**Execution focus**: Follow the 8-week roadmap sprint-by-sprint, don't skip P0/P1. Parallelization only possible after Sprint 4 (resilience) is complete.
