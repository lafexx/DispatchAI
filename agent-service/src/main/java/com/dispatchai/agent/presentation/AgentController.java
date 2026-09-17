package com.dispatchai.agent.presentation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dispatchai.agent.application.OperationsAgent;
import com.dispatchai.agent.presentation.dto.AgentRequest;
import com.dispatchai.agent.presentation.dto.AgentResponse;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

  private final OperationsAgent operationsAgent;

  public AgentController(OperationsAgent operationsAgent) {
    this.operationsAgent = operationsAgent;
  }

  @PostMapping("/operations/query")
  public AgentResponse query(@RequestBody AgentRequest request) {
    return new AgentResponse(
        operationsAgent.chat(request.message()));
  }
}
