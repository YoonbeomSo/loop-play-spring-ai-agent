package com.baedal.support.controller;

import com.baedal.support.advisor.PerformanceLoggingAdvisor;
import com.baedal.support.dto.ChatRequest;
import com.baedal.support.prompt.BaedalPrompt;
import com.baedal.support.tool.OrderTools;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

/**
 * Tool Calling이 적용된 자연어 응답 엔드포인트.
 * <p>
 * {@code /api/v1/support}가 Structured Output(JSON)을 반환하는 데 반해,
 * 이 엔드포인트는 <b>Tool 호출의 흐름을 평문으로 관찰</b>하기 위한 용도다.
 * DEBUG 로그와 함께 보면 Tool이 언제 어떻게 호출되는지 직관적으로 이해할 수 있다.
 * <p>
 * 설계: 1단계 SupportService 의 캐싱 패턴 따라 생성자에서 1회 build → 누적 빌더 bug 회피.
 *       system prompt 는 STREAMING_PROMPT (자유 텍스트 가이드, JSON 구조 미사용) 적용.
 */
@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final ChatClient chatClient;

    public AssistantController(
            ChatClient.Builder builder,
            PerformanceLoggingAdvisor performanceAdvisor,
            OrderTools orderTools
    ) {
        this.chatClient = builder
                .defaultSystem(BaedalPrompt.STREAMING_PROMPT)
                .defaultAdvisors(performanceAdvisor)
                .defaultTools(orderTools)   // Round 2 핵심 — Tool Calling
                .build();
    }

    @PostMapping
    public String ask(@Valid @RequestBody ChatRequest req) {
        return chatClient.prompt()
                .user(req.message())
                .call()
                .content();
    }
}
