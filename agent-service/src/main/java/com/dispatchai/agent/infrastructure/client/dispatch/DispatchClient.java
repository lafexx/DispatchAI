package com.dispatchai.agent.infrastructure.client.dispatch;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.dispatchai.agent.infrastructure.client.dispatch.dto.DispatchEligibilityResponse;
@Component
public class DispatchClient {
  private final RestClient client;
  public DispatchClient(RestClient.Builder builder, @Value("${services.dispatch.base-url}") String url) {
    var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
        java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build());
    factory.setReadTimeout(java.time.Duration.ofSeconds(5));
    client = builder.baseUrl(url).requestFactory(factory).build();
  }
  public DispatchEligibilityResponse eligibility(UUID id) {
    return client.get().uri("/api/dispatch/deliveries/{id}/eligibility", id).retrieve().body(DispatchEligibilityResponse.class);
  }
}
