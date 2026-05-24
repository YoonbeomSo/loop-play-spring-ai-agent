package com.example.delivery.ai;

import com.example.delivery.ai.tool.DeliveryOrderCancelTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeliveryAiAgentService {

    private final ChatClient chatClient;
    private final DeliveryOrderCancelTool deliveryOrderCancelTool;

    public String chat(Long customerId, String userMessage) {
        return chatClient.prompt()
            .system("""
                너는 배달 주문 고객센터 AI 상담원이다.

                규칙:
                - 고객이 주문 취소를 명확히 요청한 경우에만 cancelDeliveryOrder Tool을 사용한다.
                - 주문 ID가 없으면 Tool을 호출하지 말고 주문 ID를 먼저 물어본다.
                - 고객 ID는 시스템에서 전달된 값을 사용한다.
                - 쿠폰, 할인, 보상 지급을 약속하지 않는다.
                - 타 배달 플랫폼을 추천하거나 비교하지 않는다.
                - 고객, 사장님, 라이더의 개인정보를 노출하지 않는다.
                - Tool 결과가 실패면 실패 사유를 고객에게 정중하게 안내한다.
                - Tool 결과가 성공일 때만 주문 취소 완료로 안내한다.
                """)
            .user("""
                customerId: %d
                customerMessage: %s
                """.formatted(customerId, userMessage))
            .tools(deliveryOrderCancelTool)
            .call()
            .content();
    }
}
