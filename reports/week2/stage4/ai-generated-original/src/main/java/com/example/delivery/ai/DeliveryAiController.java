package com.example.delivery.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/delivery")
public class DeliveryAiController {

    private final DeliveryAiAgentService deliveryAiAgentService;

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = deliveryAiAgentService.chat(
            request.customerId(),
            request.message()
        );

        return new ChatResponse(answer);
    }

    public record ChatRequest(
        Long customerId,
        String message
    ) {
    }

    public record ChatResponse(
        String answer
    ) {
    }
}
