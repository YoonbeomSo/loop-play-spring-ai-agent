# 1단계 — Tool 구현 + 시나리오 5종 검증 보고서

> Round 2 / 1단계 — `OrderTools` 3개 Tool 구현 + Mock 데이터 4건 + 두 컨트롤러 Tool 등록.
> 진행 중 — **1차 실패 관찰 → 원인 분석 → 수정 → 2차 검증** 흐름.

## 구현 요약

### 새 파일 (upstream/round2 import + 우리 구현)

| 파일 | 역할 |
|---|---|
| `domain/Order` / `OrderItem` / `OrderStatus` / `OrderMockService` | Mock 주문 도메인 (6건 — 1234~1239) |
| `tool/OrderTools` | `@Tool` 3개 — `getOrderDetail` / `getDeliveryStatus` / `cancelOrder` |
| `tool/OrderDetailView` / `DeliveryStatusView` / `CancelOrderResult` | Tool 반환 view (LLM 노출 전용 필드만) |
| `AssistantController` | `POST /api/v1/assistant` — 자유 텍스트 + Tool Calling 통합 |

### Tool 등록 위치 (quest 명세 요구)

| 컨트롤러 | system prompt | Tool 등록 | 응답 형태 |
|---|---|---|---|
| `SupportController` → `SupportService.structuredChatClient` | `SYSTEM_PROMPT` | ✅ `OrderTools` | JSON 12필드 |
| `AssistantController` (자체 캐싱) | `STREAMING_PROMPT` | ✅ `OrderTools` | 자유 텍스트 |
| `ChatController` / `StreamingChatController` | `STREAMING_PROMPT` | ❌ | 자유 텍스트 / SSE |

설계 일관성: Round 1 의 `SupportService` 캐싱 패턴 그대로 — 생성자에서 1회 build, 누적 빌더 bug 회피.

### Mock 데이터 (`OrderMockService.seed()`)

| orderId | 상태 | 매장 | 메뉴 | 용도 |
|---|---|---|---|---|
| 1234 | DELIVERING | 교촌치킨 강남점 | 허니콤보, 콜라 | 배달 진행 + 라이더 위치 |
| 1235 | CREATED | 버거킹 선릉점 | 와퍼 세트 ×2 | 정상 CANCELED 경로 |
| 1236 | DELIVERED | 도미노피자 역삼점 | 페퍼로니 L, 콜라 ×2 | NOT_CANCELABLE 검증 |
| 1237 | COOKING | 김밥천국 강남점 | 참치김밥 ×2, 라면 | NOT_CANCELABLE 보조 |
| 1238 | CANCELED (+`.cancel()`) | BBQ치킨 강남점 | 황금올리브, 콜라 | ALREADY_CANCELED 멱등성 |
| 1239 | ACCEPTED | 맥도날드 강남점 | 빅맥 세트, 맥너겟 | 2단계 (B) 정상 취소 |

---

## 1차 시도 — Tool 호출이 발생하지 않음 ⚠️

### 검증 — quest 명세의 시나리오 5종

```bash
POST /api/v1/assistant
```

| # | 입력 | 기대 Tool | 실제 |
|---|---|---|---|
| S1 | `"주문번호 2024-1234 배달 어디쯤에 있어요?"` | `getDeliveryStatus` | ❌ Tool 호출 X |
| S2 | `"주문번호 2024-1234 어떤 메뉴 주문했어요?"` | `getOrderDetail` | ❌ |
| S3 | `"주문번호 2024-1235 방금 시킨 건데 취소해주세요"` | `cancelOrder` | ❌ |
| S4 | `"주문번호 2024-1236 취소해주세요"` | `cancelOrder` (NOT_CANCELABLE) | ❌ |
| S5 | `"주문번호 2099-9999 배달 어디예요?"` | `getDeliveryStatus` (null) | ❌ |

### 실제 응답 (LLM 자연어 — Tool 호출 없음)

**S1**:
> *"주문번호 2024-1234의 배송 상태를 확인한 뒤 안내드리겠습니다. 잠시만 기다려주세요."*

**S2**:
> *"주문번호 2024-1234의 메뉴를 확인해드리겠습니다. 잠시만 기다려주세요."*

**S3**:
> *"주문 번호 2024-1235를 확인했습니다. 잠시만 기다려 주세요. 주문을 취소하는 데 필요한 정보가 있으신가요? 아니면 '고객 요청'으로 처리하도록 하겠습니다."*

**S4**:
> *"주문 번호 2024-1236의 주문 취소 요청을 받아들였습니다. 확인 후 안내드리겠습니다. 잠시만 기다려 주세요."*

**S5**:
> *"주문번호 2099-9999의 배송 상태를 확인한 뒤 안내드리겠습니다. 잠시만 기다려주세요."*

### advisor 로그

```
[LLM] elapsedMs=10776 inputTokens=1966 outputTokens=38 totalTokens=2004
[LLM] elapsedMs=966   inputTokens=1966 outputTokens=32 totalTokens=1998
[LLM] elapsedMs=1543  inputTokens=1968 outputTokens=58 totalTokens=2026
[LLM] elapsedMs=1206  inputTokens=1962 outputTokens=44 totalTokens=2006
```

→ `[LLM]` 4건만 (호출은 됨, 1회는 timeout). **`[Tool] ...` 로그는 0건**.

### 원인 분석

quest 명세의 *"Tool이 호출되지 않을 때 확인할 것"* 체크리스트로 검증:

| # | 확인 항목 | 우리 상태 |
|---|---|---|
| 1 | `.defaultTools(orderTools)` 등록 | ✅ `AssistantController` 생성자에 등록 확인 |
| 2 | **System Prompt 의 `[Tool 사용 규칙]` 섹션** | ❌ **없음** |
| 3 | 모델 `qwen2.5` | ✅ |
| 4 | 로그 레벨 `DEBUG` | ✅ |

핵심 원인 — **`[Tool 사용 규칙]` 부재** + **`STREAMING_PROMPT` 의 예시가 Tool 회피를 유도**:

`STREAMING_PROMPT` 의 첫 예시:
```
예 1) 고객 입력: "주문번호 2024-1234 배달 어디쯤에 있어요?"
     응답: "주문번호 2024-1234 배송 상태를 확인한 뒤 안내드리겠습니다. 잠시만 기다려주세요."
```

→ **이 예시 자체가 Tool 호출 없이 *"확인 후 안내"* 회피 표현으로 답하는 패턴**. LLM 이 예시를 그대로 따라함. Round 1 의 *"보상 단정 회피"* 안전 가이드가 Round 2 에서는 *"Tool 호출 회피"* 로 역작용.

### 메타 발견

> **Round 1 에서 *안전한 어조* 를 강제했던 prompt 가 Round 2 에서 *행동 회피* 로 작용한다.**
> *"확인 후 안내드리겠습니다"* 는 Round 1 에선 **보상 약속 회피** 의 핵심 표현이었는데, Round 2 에선 **Tool 호출 회피** 의 핵심 표현이 됨.

→ prompt 의 표현 하나가 *어떤 라운드 컨텍스트에서는 안전*, *다른 컨텍스트에서는 결함* 일 수 있다. Round 1 학습 기록의 *"AI 신뢰는 모델이 아니라 구조"* 의 또 다른 사례.

---

## 수정 — `CORE_GUARDRAILS` 에 `[Tool 사용 규칙]` 추가 + `STREAMING_PROMPT` 예시 보강

(작성 중 — 수정 후 2차 검증 결과 추가 예정)

## 2차 시도 — 진행 예정

## 설계 결정 3가지 (quest 명세)

(2차 검증 후 작성 예정)

1. `OrderDetailView` 가 내부 `Order` 의 무엇을 의도적으로 뺐는가?
2. `@Tool` description 을 한국어 vs 영어, 어떤 기준으로?
3. `OrderTools` 를 한 클래스로 묶은 이유 + 분리 기준?
