package com.dispatchai.agent.presentation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dispatchai.agent.application.DispatchAdvisorService;
import com.dispatchai.agent.application.OperationsAgent;
import com.dispatchai.agent.presentation.dto.AgentRequest;
import com.dispatchai.agent.presentation.dto.AgentResponse;
import com.dispatchai.agent.presentation.dto.DispatchAdviceRequest;
import com.dispatchai.agent.presentation.dto.DispatchAdviceResponse;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

  private final OperationsAgent operationsAgent;
  private final DispatchAdvisorService dispatchAdvisorService;

  public AgentController(
      OperationsAgent operationsAgent,
      DispatchAdvisorService dispatchAdvisorService) {
    this.operationsAgent = operationsAgent;
    this.dispatchAdvisorService = dispatchAdvisorService;
  }

  @PostMapping("/operations/query")
  public AgentResponse query(@RequestBody AgentRequest request) {
    return new AgentResponse(
        operationsAgent.chat(request.message()));
  }

  @PostMapping("/dispatch/advice")
  public DispatchAdviceResponse dispatchAdvice(
      @RequestBody DispatchAdviceRequest request) {

    return dispatchAdvisorService.advise(
        request.deliveryId());
  }
}
