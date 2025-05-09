package com.practice.kafka.event;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;


public class FileEventHandler implements EventHandler {

    public static final Logger logger = LoggerFactory.getLogger(FileEventHandler.class);
    private KafkaProducer<String, String> kafkaProducer;
    private String topicName;

    private boolean isSync;

    public FileEventHandler(KafkaProducer<String, String> kafkaProducer, String topicName, boolean isSync) {
        this.kafkaProducer = kafkaProducer;
        this.topicName = topicName;
        this.isSync = isSync;
    }

    @Override
    public void onMessage(MessageEvent messageEvent) throws InterruptedException, ExecutionException {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(this.topicName, messageEvent.key, messageEvent.value);

        if (this.isSync) {
            RecordMetadata metadata = this.kafkaProducer.send(producerRecord).get();
            logger.info("""
                            ##### record metadata received #####
                            partition: {}
                            offset: {}
                            timestamp: {}
                            """,
                    metadata.partition(), metadata.offset(), metadata.timestamp());
        } else {
            this.kafkaProducer.send(producerRecord, (metadata, exception) -> {
                if (exception != null) {
                    logger.info("""
                                   \n##### record metadata received #####
                                    partition: {}
                                    offset: {}
                                    timestamp: {}
                                    """,
                            metadata.partition(), metadata.offset(), metadata.timestamp());
                } else {
                    logger.error(exception.getMessage());
                }
            });
        }
    }

    public static void main(String[] args) throws Exception {
        String topicName = "file-topic1";

        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);
        boolean isSync = true;

        FileEventHandler fileEventHandler = new FileEventHandler(kafkaProducer, topicName, isSync);
        MessageEvent messageEvent = new MessageEvent("key001", "this is test message");
        fileEventHandler.onMessage(messageEvent);
    }

}
