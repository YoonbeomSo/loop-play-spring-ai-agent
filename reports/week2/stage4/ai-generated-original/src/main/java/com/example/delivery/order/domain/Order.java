package com.example.delivery.order.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "orders")
public class Order {

    @Id
    private Long id;

    private Long customerId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String cancelReason;

    private String canceledBy;

    public boolean isOwnedBy(Long customerId) {
        return this.customerId.equals(customerId);
    }

    public boolean isCancelable() {
        return status == OrderStatus.ORDERED
            || status == OrderStatus.ACCEPTED;
    }

    public void cancel(String cancelReason, String canceledBy) {
        if (!isCancelable()) {
            throw new IllegalStateException("취소할 수 없는 주문 상태입니다.");
        }

        this.status = OrderStatus.CANCELED;
        this.cancelReason = cancelReason;
        this.canceledBy = canceledBy;
    }
}
