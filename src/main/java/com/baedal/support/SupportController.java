package com.baedal.support;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/support")
public class SupportController {

    private final ChatClient.Builder builder;

    @PostMapping
    public SupportResponse triage(@RequestBody ChatRequest req) {
        return builder
                .defaultSystem(BaedalPrompt.SYSTEM_PROMPT)
                .build()
                .prompt()
                .user(req.message())
                .call()
                .entity(SupportResponse.class);
    }
}
