package com.ymall.backend.global.messaging.outbox;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderOutboxEventRepository extends JpaRepository<OrderOutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select event
        from OrderOutboxEvent event
        where event.status in :statuses
          and event.nextAttemptAt <= :now
        order by event.createdAt, event.eventId
        """)
    List<OrderOutboxEvent> findPublishable(
        @Param("statuses") Collection<OutboxEventStatus> statuses,
        @Param("now") Instant now,
        Pageable pageable
    );

    @Modifying
    @Query("""
        delete from OrderOutboxEvent event
        where event.status = :status
          and event.publishedAt < :cutoff
        """)
    int deletePublishedBefore(
        @Param("status") OutboxEventStatus status,
        @Param("cutoff") Instant cutoff
    );
}
