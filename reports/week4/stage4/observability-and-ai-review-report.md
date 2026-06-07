# 4단계 — Observability (RAG 토큰 관찰)

> RAG 가 주입하는 토큰 비용을 (a)(b)(c) 3조건으로 직접 관찰한다. 측정 질문(단일턴):
> `"배달 완료 후에도 환불 받을 수 있나요?"` / temp 0.3, qwen2.5 + qwen3-embedding:0.6b.

## A. RAG 주입 토큰 비용 (Phase 4-1)

`AssistantChatClientConfig` 의 `defaultAdvisors` 를 임시 수정해 advisor 체인을 3조건으로 두고 같은 질문 측정:

| 조건 | Advisor 체인 | 입력 토큰 | 출력 토큰 | 응답시간(ms) | 메시지 수 |
|---|---|---:|---:|---:|---:|
| (a) Memory·RAG 없음 | performance | **3889** | 65 | 1741 | 2 (SYSTEM+USER) |
| (b) Memory만 | memory, performance | **3889** | 126 | 14378 | 2 (빈 Memory) |
| (c) Memory+RAG | memory, rag, performance | **4096** | 26 | 12588 | 2 (USER+Context) |

**관찰:**
- **(a) = (b) = 3889** — 단일턴이라 Memory 가 비어 있어 토큰을 0 추가. "빈 Memory 는 비용이 없다"를 수치로 확인.
- **(c) = 4096** — RAG 가 환불 정책 Context 를 주입해 (a) 대비 **+207** 증가. 그런데 주입된 문서(`refund-after-delivered`)는
  실제 ~400~500 토큰이다. **(c) 가 정확히 4096 인 건 Ollama `num_ctx` 기본 천장(4096)에 닿아 잘렸다는 신호** —
  즉 **RAG 의 진짜 토큰 비용이 천장에 가려 +207 로 과소 측정**됐다(2단계와 동일한 num_ctx 한계).
- 응답시간은 CPU 추론 + 워밍업 영향으로 편차가 크다((a) 1.7s 는 캐시 효과). 입력 토큰이 더 신뢰할 지표.
- 결론: "RAG 는 정확도를 토큰으로 산다"가 맞지만, **이 환경에선 system prompt(~3500)가 이미 천장의 대부분을
  차지**해 RAG 증분이 천장에 묻힌다. 토큰 비용을 제대로 보려면 `num_ctx` 를 키우거나 system prompt 를 줄여야 한다.

## B. Context 블록 원문 캡처 (Phase 4-2)

조건 (c) DEBUG 로그 — QA Advisor 가 USER 메시지에 주입한 Context:
```
#0 [SYSTEM]  [역할] 당신은 배달 서비스의 ...
#1 [USER]    배달 완료 후에도 환불 받을 수 있나요?

             아래는 참고용 배달 정책 문서입니다. (질문과 무관하면 무시하세요)
             ---------------------
             # 배달 완료 후 환불 정책
             ## 배달 완료 후 환불 가능 사유
             - 음식 누락 / 오배송 ... (refund-after-delivered 원문)
             ---------------------
```
→ 질문("배달 완료 후 환불")에 `refund-after-delivered` 문서가 threshold 0.5 를 넘겨 정확히 매칭·주입됐다.
이 Context 블록이 (a) 대비 입력 토큰을 늘린 실체다(단, 위 A 처럼 천장에 일부 잘림).

## 학습 기록

→ Round 4 공통 학습 기록은 [README](../../../README.md) 의 *Round 4 — 공통 학습 기록* 참조.
