package com.baedal.support;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class SupportService {

    /** Structured Output(JSON 12필드) 용. `BaedalPrompt.SYSTEM_PROMPT` 적용. */
    private final ChatClient structuredChatClient;

    /** Streaming(자유 텍스트) 용. `BaedalPrompt.STREAMING_PROMPT` 적용. */
    private final ChatClient streamingChatClient;

    public SupportService(
            ChatClient.Builder structuredBuilder,
            ChatClient.Builder streamingBuilder,
            PerformanceLoggingAdvisor performanceAdvisor
    ) {
        this.structuredChatClient = structuredBuilder
                .defaultSystem(BaedalPrompt.SYSTEM_PROMPT)
                .defaultAdvisors(performanceAdvisor)
                .build();
        this.streamingChatClient = streamingBuilder
                .defaultSystem(BaedalPrompt.STREAMING_PROMPT)
                .defaultAdvisors(performanceAdvisor)
                .build();
    }

    /** 동기 호출 — Structured Output(JSON 12필드)으로 응답을 받는다. */
    public SupportResponse generateSupportResponse(String message) {
        return structuredChatClient
                .prompt()
                .user(message)
                .call()
                .entity(SupportResponse.class);
    }

    /**
     * 스트리밍 호출 — 고객에게 보일 자연어 응답만 토큰 단위로 흘려보낸다.
     * `STREAMING_PROMPT` 가 JSON 구조를 금지하므로 사용자 화면에 노출 가능한 자연어 한 단락만 옴.
     */
    public Flux<String> streamSupportResponse(String message) {
        return streamingChatClient
                .prompt()
                .user(message)
                .stream()
                .content();
    }

    /**
     * 동기 자유 텍스트 호출 — streaming 과 동일한 `STREAMING_PROMPT` 적용.
     * `/api/v1/chat` 에서 사용. 동기 vs streaming 의 *순수* 차이 비교를 위해 같은 prompt 공유.
     */
    public String chat(String message) {
        return streamingChatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }
}
