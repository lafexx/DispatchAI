package com.dispatchai.analytics.domain.model;
/** Distinct delivery activity within an event-time window. */
public record WindowActivity(long active, long delayed, long created, long completed) {
  public static WindowActivity empty() { return new WindowActivity(0, 0, 0, 0); }
  public WindowActivity apply(DeliveryStateUpdate u) {
    return new WindowActivity(1, delayed == 1 || Boolean.TRUE.equals(u.delayed()) ? 1 : 0,
        created == 1 || u.status() == AnalyticsDeliveryStatus.PENDING ? 1 : 0,
        completed == 1 || u.status() == AnalyticsDeliveryStatus.COMPLETED ? 1 : 0);
  }
  public WindowActivity plus(WindowActivity v) { return new WindowActivity(active+v.active, delayed+v.delayed, created+v.created, completed+v.completed); }
  public WindowActivity minus(WindowActivity v) { return new WindowActivity(active-v.active, delayed-v.delayed, created-v.created, completed-v.completed); }
}
