package com.dispatchai.agent.presentation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dispatchai.agent.application.DispatchAdvisorAgent;
import com.dispatchai.agent.application.OperationsAgent;
import com.dispatchai.agent.presentation.dto.AgentRequest;
import com.dispatchai.agent.presentation.dto.AgentResponse;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

  private final OperationsAgent operationsAgent;
  private final DispatchAdvisorAgent dispatchAdvisorAgent;

  public AgentController(
      OperationsAgent operationsAgent,
      DispatchAdvisorAgent dispatchAdvisorAgent) {
    this.operationsAgent = operationsAgent;
    this.dispatchAdvisorAgent = dispatchAdvisorAgent;
  }

  @PostMapping("/operations/query")
  public AgentResponse query(@RequestBody AgentRequest request) {
    return new AgentResponse(
        operationsAgent.chat(request.message()));
  }

  @PostMapping("/dispatch/advice")
  public AgentResponse dispatchAdvice(@RequestBody AgentRequest request) {
    return new AgentResponse(
        dispatchAdvisorAgent.advise(request.message()));
  }
}
