package com.dispatchai.analytics.domain.model;
import java.time.Instant;
import java.time.Duration;
public record SummaryAggregate(long total, long pending, long assigned, long inTransit, long completed, long cancelled, long delayed, long delaySum, long delayCount, long assignmentMs, long assignmentCount, long deliveryMs, long deliveryCount, long transitMs, long transitCount) {
  public static SummaryAggregate empty() { return new SummaryAggregate(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0); }
  public static SummaryAggregate contribution(DeliveryAnalyticsState d) { return new SummaryAggregate(1, d.status() == AnalyticsDeliveryStatus.PENDING ? 1 : 0, d.status() == AnalyticsDeliveryStatus.ASSIGNED ? 1 : 0, d.status() == AnalyticsDeliveryStatus.IN_TRANSIT ? 1 : 0, d.status() == AnalyticsDeliveryStatus.COMPLETED ? 1 : 0, d.status() == AnalyticsDeliveryStatus.CANCELLED ? 1 : 0, d.delayed() ? 1 : 0, d.delayMinutes() == null ? 0 : d.delayMinutes(), d.delayMinutes() == null ? 0 : 1, millis(d.createdAt(), d.assignedAt()), valid(d.createdAt(), d.assignedAt()) ? 1 : 0, millis(d.createdAt(), d.completedAt()), valid(d.createdAt(), d.completedAt()) ? 1 : 0, millis(d.startedAt(), d.completedAt()), valid(d.startedAt(), d.completedAt()) ? 1 : 0); }
  public SummaryAggregate combine(SummaryAggregate v, int sign) { return new SummaryAggregate(total + sign * v.total, pending + sign * v.pending, assigned + sign * v.assigned, inTransit + sign * v.inTransit, completed + sign * v.completed, cancelled + sign * v.cancelled, delayed + sign * v.delayed, delaySum + sign * v.delaySum, delayCount + sign * v.delayCount, assignmentMs + sign * v.assignmentMs, assignmentCount + sign * v.assignmentCount, deliveryMs + sign * v.deliveryMs, deliveryCount + sign * v.deliveryCount, transitMs + sign * v.transitMs, transitCount + sign * v.transitCount); }
  private static boolean valid(Instant a, Instant b) { return a != null && b != null && !b.isBefore(a); }
  private static long millis(Instant a, Instant b) { return valid(a,b) ? Duration.between(a,b).toMillis() : 0; }
  private static double ratio(long a, long b) { return b == 0 ? 0 : (double)a/b; }
  public double averageDelayMinutes() { return ratio(delaySum, delayCount); }
  public double averageAssignmentTimeSeconds() { return ratio(assignmentMs, assignmentCount)/1000; }
  public double averageDeliveryTimeSeconds() { return ratio(deliveryMs, deliveryCount)/1000; }
  public double averageTransitTimeSeconds() { return ratio(transitMs, transitCount)/1000; }
  public double completionRate() { return ratio(completed, total); }
  public double cancellationRate() { return ratio(cancelled, total); }
  public double delayRate() { return ratio(delayed, total); }
}
