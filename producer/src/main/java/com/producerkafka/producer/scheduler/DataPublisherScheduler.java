package com.producerkafka.producer.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.producerkafka.producer.KafkaProducerService;
import com.producerkafka.producer.service.TrainDataGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
public class DataPublisherScheduler {
    
    private static final long PUBLISH_RATE_MS = 5000; // 5 seconds
    
    private final TrainDataGenerator dataGenerator;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;
    
    public DataPublisherScheduler(TrainDataGenerator dataGenerator, 
                                  KafkaProducerService kafkaProducerService) {
        this.dataGenerator = dataGenerator;
        this.kafkaProducerService = kafkaProducerService;
        
        // Configure ObjectMapper for LocalDateTime serialization
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * Runs when the application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Application started. Train data publisher is ready.");
        log.info("Publishing train data every {} ms to Kafka topic: train-date", PUBLISH_RATE_MS);
    }
    
    /**
     * Scheduled method that generates and publishes train data
     * Runs every 5 seconds (fixed rate)
     */
    @Scheduled(fixedRate = PUBLISH_RATE_MS)
    public void publishTrainData() {
        try {
            // Generate random train data
            Object trainData = dataGenerator.generateRandomData();
            String dataType = trainData.getClass().getSimpleName();
            
            // Serialize to JSON
            String jsonData = objectMapper.writeValueAsString(trainData);
            
            // Send to Kafka with data type as key
            kafkaProducerService.sendMessage(dataType, jsonData);
            
            log.info("Published {} to Kafka (size: {} bytes)", dataType, jsonData.length());
            
        } catch (Exception e) {
            log.error("Error publishing train data to Kafka", e);
        }
    }
}

