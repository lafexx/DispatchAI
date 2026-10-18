package com.dispatchai.analytics.domain.model;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class AnalyticsProjectionTest {
  private final Instant base = Instant.parse("2026-10-13T10:00:00Z");
  private DeliveryStateUpdate event(AnalyticsDeliveryStatus status, Boolean delayed, Integer minutes, String reason, int seconds) {
    return new DeliveryStateUpdate(UUID.randomUUID(), null, null, status, delayed, minutes, reason, base.plusSeconds(seconds), UUID.randomUUID());
  }
  @Test void lateCreationDoesNotEraseDelay() {
    var state = DeliveryAnalyticsState.empty().apply(event(null,true,10,"traffic",30))
        .apply(event(AnalyticsDeliveryStatus.PENDING,false,null,null,0));
    assertTrue(state.delayed()); assertEquals(10,state.delayMinutes()); assertEquals(base,state.createdAt());
  }
  @Test void olderDelayDoesNotOverwriteNewerDetails() {
    var state = DeliveryAnalyticsState.empty().apply(event(null,true,10,"new",30)).apply(event(null,true,5,"old",10));
    assertEquals(10,state.delayMinutes()); assertEquals("new",state.delayReason());
  }
  @Test void terminalStateSurvivesLateAssignmentButTimestampIsRecovered() {
    var state = DeliveryAnalyticsState.empty().apply(event(AnalyticsDeliveryStatus.COMPLETED,null,null,null,120))
        .apply(event(AnalyticsDeliveryStatus.ASSIGNED,null,null,null,60));
    assertEquals(AnalyticsDeliveryStatus.COMPLETED,state.status()); assertEquals(base.plusSeconds(60),state.assignedAt());
  }
  @Test void summarySubtractsOldContributionBeforeAddingNewProjection() {
    var pending = DeliveryAnalyticsState.empty().apply(event(AnalyticsDeliveryStatus.PENDING,false,null,null,0));
    var completed = pending.apply(event(AnalyticsDeliveryStatus.COMPLETED,null,null,null,120));
    var summary = SummaryAggregate.empty().combine(SummaryAggregate.contribution(pending),1)
        .combine(SummaryAggregate.contribution(pending),-1).combine(SummaryAggregate.contribution(completed),1);
    assertEquals(1,summary.total()); assertEquals(0,summary.pending());
    assertEquals(1,summary.completed()); assertEquals(120,summary.averageDeliveryTimeSeconds());
  }
  @Test void negativeDurationsAreExcluded() {
    var state = DeliveryAnalyticsState.empty().apply(event(AnalyticsDeliveryStatus.PENDING,false,null,null,120))
        .apply(event(AnalyticsDeliveryStatus.COMPLETED,null,null,null,0));
    assertEquals(0,SummaryAggregate.contribution(state).averageDeliveryTimeSeconds());
  }
  @Test void repeatedDelayEventsCountOneDelivery() {
    var e = event(null,true,10,"traffic",30);
    assertEquals(new WindowActivity(1,1,0,0),WindowActivity.empty().apply(e).apply(e));
  }
}
