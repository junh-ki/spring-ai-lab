package com.example.springailab.creativechat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class CreativeChatController {

    private final CreativeChatService creativeChatService;

    @GetMapping("/poem")
    public String writePoem(@RequestParam final String topic) {
        return this.creativeChatService.writePoem(topic);
    }
}
