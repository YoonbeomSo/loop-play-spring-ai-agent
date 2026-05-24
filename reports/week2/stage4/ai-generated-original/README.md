# Spring AI 1.0 배달 주문 취소 Tool 예제

Spring AI 1.0의 `@Tool` 어노테이션을 사용해 배달 주문 취소 기능을 LLM Tool로 연결하는 예제입니다.

핵심 구조는 다음과 같습니다.

```text
LLM
→ cancelDeliveryOrder Tool 호출 결정
→ DeliveryOrderCancelTool
→ OrderCancelService
→ Order 도메인 검증
→ DB 상태 변경
→ 결과를 LLM에게 반환
→ LLM이 고객에게 안내
```

## 핵심 원칙

- LLM이 직접 DB를 수정하지 않습니다.
- `@Tool` 메서드는 도메인 서비스를 호출하는 진입점 역할만 합니다.
- 주문 취소 가능 여부는 `Order` 도메인과 `OrderCancelService`에서 검증합니다.
- Tool 결과가 `success=true`일 때만 고객에게 취소 완료를 안내해야 합니다.
- 고객 ID는 사용자가 말한 값이 아니라 로그인 세션/JWT에서 꺼낸 서버 값을 사용하는 것이 안전합니다.

## Gradle 의존성 예시

```gradle
dependencies {
    implementation platform("org.springframework.ai:spring-ai-bom:1.0.0")
    implementation "org.springframework.ai:spring-ai-starter-model-openai"

    implementation "org.springframework.boot:spring-boot-starter-web"
    implementation "org.springframework.boot:spring-boot-starter-validation"
    implementation "org.springframework.boot:spring-boot-starter-data-jpa"
}
```

## 포함 파일

```text
src/main/java/com/example/delivery/ai/tool/DeliveryOrderCancelTool.java
src/main/java/com/example/delivery/ai/DeliveryAiAgentService.java
src/main/java/com/example/delivery/ai/DeliveryAiController.java
src/main/java/com/example/delivery/order/application/OrderCancelService.java
src/main/java/com/example/delivery/order/application/dto/CancelOrderCommand.java
src/main/java/com/example/delivery/order/application/dto/CancelOrderResult.java
src/main/java/com/example/delivery/order/domain/Order.java
src/main/java/com/example/delivery/order/domain/OrderStatus.java
src/main/java/com/example/delivery/order/domain/OrderRepository.java
```
