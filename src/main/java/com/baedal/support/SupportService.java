package com.baedal.support;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class SupportService {

    private final ChatClient chatClient;

    public SupportService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(BaedalPrompt.SYSTEM_PROMPT)
                .build();
    }

    public SupportResponse generateSupportResponse(String message) {
        return chatClient
                .prompt()
                .user(message)
                .call()
                .entity(SupportResponse.class);
    }
}
