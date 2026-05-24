package com.example.delivery.order.application;

import com.example.delivery.order.application.dto.CancelOrderCommand;
import com.example.delivery.order.application.dto.CancelOrderResult;
import com.example.delivery.order.domain.Order;
import com.example.delivery.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCancelService {

    private final OrderRepository orderRepository;

    @Transactional
    public CancelOrderResult cancel(CancelOrderCommand command) {
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (!order.isOwnedBy(command.customerId())) {
            return CancelOrderResult.fail(
                order.getId(),
                order.getStatus().name(),
                "본인 주문만 취소할 수 있습니다."
            );
        }

        if (!order.isCancelable()) {
            return CancelOrderResult.fail(
                order.getId(),
                order.getStatus().name(),
                "현재 주문 상태에서는 취소가 어렵습니다. 매장 확인이 필요합니다."
            );
        }

        order.cancel(command.cancelReason(), command.requestedBy());

        return CancelOrderResult.success(
            order.getId(),
            order.getStatus().name(),
            "주문이 정상적으로 취소되었습니다."
        );
    }
}
