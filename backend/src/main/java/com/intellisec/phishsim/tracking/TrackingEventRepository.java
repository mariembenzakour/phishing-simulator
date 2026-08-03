package com.intellisec.phishsim.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, UUID> {
    List<TrackingEvent> findBySendEventId(UUID sendEventId);
    List<TrackingEvent> findBySendEventIdAndEventType(UUID sendEventId, String eventType);
}