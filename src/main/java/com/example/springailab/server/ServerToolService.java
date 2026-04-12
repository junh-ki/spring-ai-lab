package com.example.springailab.server;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class ServerToolService {

    @Tool(description = "Request a server restart.")
    public String requestRestart(final String serverId) {
        final String approvalLink = "https://admin.portal/approve/" + serverId;
        // notificationService.sendApprovalRequest(approvalLink); // Send email/Slack to manager
        return "I have sent an approval request to your manager. "
            + "Once they click the link, please tell me 'It is approved'.";
    }
}
