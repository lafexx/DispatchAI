package com.dispatchai.agent.application;

import com.dispatchai.agent.infrastructure.ai.agent.DispatchAdvisorAgent;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import com.dispatchai.agent.application.model.DispatchAdviceDraft;
import com.dispatchai.agent.application.model.DispatchCandidate;
import com.dispatchai.agent.application.model.DispatchRecommendationContext;
import com.dispatchai.agent.domain.exception.InvalidDispatchRecommendationException;
import com.dispatchai.agent.infrastructure.client.fleet.dto.DriverResponse;
import com.dispatchai.agent.infrastructure.client.delivery.dto.DeliveryResponse;
import com.dispatchai.agent.infrastructure.client.fleet.dto.VehicleResponse;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DispatchAdvisorServiceTest {
  @Mock DispatchRecommendationContextService contexts;
  @Mock DispatchAdvisorAgent agent;
  @Mock ObjectMapper mapper;
  @InjectMocks DispatchAdvisorService service;
  UUID deliveryId=UUID.randomUUID(),driverId=UUID.randomUUID(),vehicleId=UUID.randomUUID();
  DispatchRecommendationContext context;
  DispatchAdviceDraft valid() { return new DispatchAdviceDraft(driverId.toString(),"Invented name","Fits the parcel",vehicleId.toString()); }
  @BeforeEach void setup() {
    var delivery=new DeliveryResponse(deliveryId,UUID.randomUUID(),null,"P","D","PENDING",false,null,null,new BigDecimal("50"));
    var driver=new DriverResponse(driverId,"Actual driver","AVAILABLE");
    var vehicle=new VehicleResponse(vehicleId,"ACTUAL","VAN",new BigDecimal("100"),true);
    context=new DispatchRecommendationContext(delivery,List.of(new DispatchCandidate(driver,null)),List.of(vehicle));
    lenient().when(contexts.get(deliveryId)).thenReturn(context);
    lenient().when(mapper.writeValueAsString(any())).thenReturn("{}");
  }
  @Test void returnsAuthoritativeNamesCapacityAndEvidenceWithoutAssigning() {
    when(agent.advise(anyString())).thenReturn(valid());
    var result=service.advise(deliveryId);
    assertEquals("Actual driver",result.recommendedDriverName()); assertEquals("ACTUAL",result.recommendedVehicleRegistration());
    assertEquals(new BigDecimal("50"),result.parcelWeightKg()); assertEquals(1,result.candidates().size());
    verify(contexts,times(2)).get(deliveryId);
  }
  @Test void nullOutputIsRejected() {
    when(agent.advise(anyString())).thenReturn(null);
    assertThrows(InvalidDispatchRecommendationException.class,()->service.advise(deliveryId));
  }
  @Test void nullOrMalformedIdsAreRejected() {
    when(agent.advise(anyString())).thenReturn(new DispatchAdviceDraft(null,"name","reason","bad"));
    assertThrows(InvalidDispatchRecommendationException.class,()->service.advise(deliveryId));
  }
  @Test void inventedVehicleIsRejected() {
    when(agent.advise(anyString())).thenReturn(new DispatchAdviceDraft(driverId.toString(),"name","reason",UUID.randomUUID().toString()));
    assertThrows(InvalidDispatchRecommendationException.class,()->service.advise(deliveryId));
  }
  @Test void inventedDriverIsRejected() {
    when(agent.advise(anyString())).thenReturn(new DispatchAdviceDraft(UUID.randomUUID().toString(),"name","reason",vehicleId.toString()));
    assertThrows(InvalidDispatchRecommendationException.class,()->service.advise(deliveryId));
  }
  @Test void emptyExplanationIsRejected() {
    when(agent.advise(anyString())).thenReturn(new DispatchAdviceDraft(driverId.toString(),"name"," ",vehicleId.toString()));
    assertThrows(InvalidDispatchRecommendationException.class,()->service.advise(deliveryId));
  }
  @Test void noVehicleAvoidsInference() {
    when(contexts.get(deliveryId)).thenReturn(new DispatchRecommendationContext(context.delivery(),context.availableDrivers(),List.of()));
    assertThrows(ResponseStatusException.class,()->service.advise(deliveryId)); verifyNoInteractions(agent);
  }
  @Test void staleVehicleIsRejectedAfterInference() {
    when(agent.advise(anyString())).thenReturn(valid());
    when(contexts.get(deliveryId)).thenReturn(context,new DispatchRecommendationContext(context.delivery(),context.availableDrivers(),List.of()));
    assertThrows(ResponseStatusException.class,()->service.advise(deliveryId));
  }
  @Test void modelTimeoutHasGatewayTimeoutStatus() {
    when(agent.advise(anyString())).thenThrow(new RuntimeException(new java.net.http.HttpTimeoutException("Timed out")));
    var error=assertThrows(ResponseStatusException.class,()->service.advise(deliveryId));
    assertEquals(504,error.getStatusCode().value());
  }
  @Test void changedParcelWeightInvalidatesAdvice() {
    when(agent.advise(anyString())).thenReturn(valid());
    var d=context.delivery();
    var changed=new DeliveryResponse(d.id(),d.customerId(),d.assignedDriverId(),d.pickupAddress(),d.destinationAddress(),d.status(),d.delayed(),d.delayMinutes(),d.delayReason(),new BigDecimal("60"));
    when(contexts.get(deliveryId)).thenReturn(context,new DispatchRecommendationContext(changed,context.availableDrivers(),context.eligibleVehicles()));
    assertThrows(ResponseStatusException.class,()->service.advise(deliveryId));
  }
}
