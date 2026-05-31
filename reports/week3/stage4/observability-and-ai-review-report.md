# 4단계 — Observability + AI 코드 리뷰

> Memory 가 토큰 비용에 미치는 영향을 로그로 관찰하고(Observability), AI 가 생성한 멀티턴 챗봇
> 코드의 프로덕션 결함을 Round 3 자산으로 개선한다(AI 코드 리뷰).

## A. Observability — 10턴 토큰 증가 + Memory 주입 관찰

### 측정 조건

- `MAX_MESSAGES=20`, 기본 프로필(InMemory), temperature 0.3
- 한 세션에서 10턴 연속 대화 (`PerformanceLoggingAdvisor` 로그로 턴별 토큰 수집)
- 프롬프트 전문은 advisor 에 추가한 DEBUG dump 로 캡처

### 10턴 입력 토큰 증가

| 턴 | messages | inputTokens | outputTokens | 비고 |
|---:|---:|---:|---:|---|
| 1 | 2 | 3559 | 50 | Memory 비어있음 (SYSTEM + USER) |
| 2 | 4 | 3643 | 52 | Memory 가 1턴 주입 |
| 3 | 6 | 3711 | 61 | |
| 4 | 8 | 3790 | 95 | |
| 5 | 10 | 3904 | 74 | |
| 6 | 12 | 3995 | 61 | |
| 7 | 14 | 4075 | 86 | |
| 8 | 16 | 4090 | 43 | 윈도우 상한 근접 |
| 9 | 18 | 4093 | 72 | |
| 10 | 18 | 4090 | 26 | 정체 (≈4090) |

**관찰:**

- **입력 토큰이 3559 → 4093 으로 단조 증가** (10턴에 +534, 턴당 약 +53). Memory 가 이전 대화를
  프롬프트에 누적하기 때문 — *Memory 작동 = 입력 토큰 증가* 를 수치로 확인.
- **messages 가 2 → 18 로 증가**하다 T8~10 에서 토큰이 ~4090 으로 **정체**. `MAX_MESSAGES=20` 의
  슬라이딩 윈도우가 상한에 닿아 더 오래된 메시지를 잘라내기 시작한 신호. (2단계 MAX_MESSAGES 결론과 일치:
  윈도우가 차면 토큰이 평평해진다. 무제한이었다면 계속 증가했을 것.)
- **기저값 ~3500** 의 대부분은 system prompt + Tool 정의 JSON 이다 (Round 2 4단계 측정: system 70% +
  tools 28%). Memory 가 더하는 건 그 위의 증분 — *Memory 자체보다 system+tools 가 토큰의 주범* 이라는
  Round 2 결론이 여기서도 유효하다.

### 2회차 프롬프트 전문 — Memory 주입의 실물

1회차는 `messages=2`(SYSTEM + USER), 2회차는 `messages=4`. advisor DEBUG dump 로 본 2회차 프롬프트:

```
#0 [USER]      2024-1234 배달 어디쯤이에요?                    ← 1회차 USER  (Memory 가 주입)
#1 [ASSISTANT] 주문 2024-1234는 현재 배달 중이며 ... 약 20분 후 ← 1회차 ASSISTANT (Memory 가 주입)
#2 [SYSTEM]    [역할] 당신은 배달 서비스의 고객 상담 AI ...      ← system prompt
#3 [USER]      그거 몇 분 남았어요?                            ← 2회차 USER (이번 요청)
```

→ **2회차 요청에 1회차 대화(USER+ASSISTANT)가 그대로 들어간다.** "그거"가 1234 로 풀리는 건 마법이 아니라
이 주입의 결과다. 1단계에서 "Memory 검증과 Tool 검증은 별개"라 했는데, 그 *Memory 검증* 의 내부 동작이 이것.
(TOOL 메시지는 안 들어감 — `MessageChatMemoryAdvisor` 가 USER/ASSISTANT 만 적재, 3단계 스키마 관찰과 일치.)

## B. AI 코드 리뷰 (멀티턴 챗봇)

> **상태: 실제 AI 생성 코드 입력 대기.**
> Round 2 가 실제 GPT-5.5 원본을 분석해 부분점수 1위 영역이 된 패턴을 따라, 이번에도 실제 AI 에
> 아래 프롬프트를 던져 받은 코드를 `reports/week3/stage4/ai-generated-original/`(.gitignore)에 보관하고
> 분석한다.
>
> 던진 프롬프트:
> ```
> Spring AI 1.0으로 멀티턴 대화 챗봇을 만들어줘.
> ChatMemory를 써서 이전 대화를 기억하게 해줘. 컨트롤러랑 설정 코드까지 보여줘.
> ```

분석 시 볼 **프로덕션 결함 후보** (Round 3 에서 우리가 직접 겪은 함정들과 매핑):

| 결함 후보 | 우리 라운드에서의 근거 | 개선 방향 |
|---|---|---|
| 세션 ID 누락 (전역 `default` 공유) | 1단계 시나리오 5 — 헤더 없으면 고객 대화가 섞임(개인정보 사고) | `X-Session-Id` → `CONVERSATION_ID`, 프로덕션 400 |
| 크기 제한 없음 (무제한 Memory) | 2단계 — MAX_VALUE 면 입력 토큰 선형 증가 | `MessageWindowChatMemory(maxMessages=N)` |
| InMemory 영구 사용 | 3단계 재시작 실험 — 배포 한 번에 전체 소실 | JDBC + 재시작 생존 (의사결정 트리) |
| DB 평문 저장 | 3단계 — content 에 전화번호·주소 평문 | 마스킹 + TTL (Round 5) |
| Advisor 순서 미고려 | 1단계 — memory(10) < performance(100) | order 명시 |
| Builder 요청마다 build | Round 2 — Multiple tools 누적 오류 | ChatClient 빈 1회 조립 |
| 맥락 규칙 prompt 과신 | 2단계 ablation — 경쟁 섹션이 Tool 호출 억제 | 모델이 이미 하는 일은 명시 안 함 |

→ 실제 코드를 받으면 이 중 **실제로 존재하는 결함 3개 이상** 을 골라 *원본 인용 + 우리 라운드 도구로의 개선 코드* 를 작성한다.

## 학습 기록

> (본인 언어로 작성 — 아래는 빈칸)

- **내가 배운 것**:
- **의문점**:
- **Round 4(RAG)에 시도하고 싶은 것**:
