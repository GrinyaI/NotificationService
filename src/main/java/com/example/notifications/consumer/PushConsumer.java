package com.example.notifications.consumer;

import com.example.notifications.entity.enums.Channel;
import com.example.notifications.service.NotificationDeliveryProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PushConsumer {

    private final NotificationDeliveryProcessor deliveryProcessor;

    @KafkaListener(
            topics = "${kafka.topic.push}",
            groupId = "${kafka.consumer.group.push}"
    )
    public void listen(
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Payload String value
    ) {
        deliveryProcessor.process(Channel.PUSH, key, value);
    }
}
