package com.example.delivery.ai.tool;

import com.example.delivery.order.application.OrderCancelService;
import com.example.delivery.order.application.dto.CancelOrderCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryOrderCancelTool {

    private final OrderCancelService orderCancelService;

    @Tool(
        name = "cancelDeliveryOrder",
        description = """
            배달 주문을 취소하는 Tool입니다.
            고객이 주문 취소를 요청했을 때만 사용합니다.
            주문 ID, 고객 ID, 취소 사유가 필요합니다.
            이미 조리 중이거나 배달 중인 주문은 정책에 따라 취소가 거절될 수 있습니다.
            쿠폰, 보상, 환불 금액을 임의로 약속하지 않습니다.
            """
    )
    public CancelOrderToolResponse cancelDeliveryOrder(
        @ToolParam(description = "취소할 주문 ID") Long orderId,
        @ToolParam(description = "취소를 요청한 고객 ID") Long customerId,
        @ToolParam(description = "고객이 말한 주문 취소 사유") String cancelReason
    ) {
        CancelOrderCommand command = new CancelOrderCommand(
            orderId,
            customerId,
            cancelReason,
            "AI_AGENT"
        );

        var result = orderCancelService.cancel(command);

        return new CancelOrderToolResponse(
            result.success(),
            result.orderId(),
            result.orderStatus(),
            result.message()
        );
    }

    public record CancelOrderToolResponse(
        boolean success,
        Long orderId,
        String orderStatus,
        String message
    ) {
    }
}
