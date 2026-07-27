package com.ymall.backend.settlement.event;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ymall.backend.global.messaging.OrderEventEnvelope;
import com.ymall.backend.settlement.service.SettlementLedgerProcessor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"ymall.kafka.enabled", "ymall.kafka.settlement-consumer.enabled"},
    havingValue = "true"
)
public class SettlementLedgerConsumer {

    private final SettlementLedgerProcessor processor;

    @KafkaListener(
        topics = "${ymall.kafka.order-events.name}",
        groupId = "${ymall.kafka.settlement-consumer.group-id}"
    )
    public void consume(OrderEventEnvelope event) {
        processor.process(event);
    }
}
