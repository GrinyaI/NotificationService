package com.example.notifications.config;

import com.example.notifications.service.NotificationProcessingFailureService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;
    @Value("${kafka.consumer.retry.backoff-ms}")
    private long retryBackoffMs;
    @Value("${kafka.consumer.retry.max-attempts}")
    private long retryMaxAttempts;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NotificationProcessingFailureService failureService;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            DefaultErrorHandler kafkaErrorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DeadLetterPublishingRecoverer dltRecoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".dlt", record.partition())
        );
        ConsumerRecordRecoverer recoverer = (record, exception) -> recover(record, exception, dltRecoverer);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(retryBackoffMs, retryMaxAttempts)
        );
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }

    private void recover(
            ConsumerRecord<?, ?> record,
            Exception exception,
            DeadLetterPublishingRecoverer dltRecoverer) {
        dltRecoverer.accept(record, exception);
        failureService.markFailed(String.valueOf(record.key()), exception);
    }
}
