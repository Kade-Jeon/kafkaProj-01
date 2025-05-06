package com.example.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class SimpleProducerSync {
    public static final Logger logger = LoggerFactory.getLogger(SimpleProducerSync.class);

    public static void main(String[] args) {
        String topicName = "simple-topic";

        //KafkaProducer Configuration Setting
        Properties props = new Properties();
        // bootstrap.servers
        // key.serializer.class
        // value.serializer.class
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // KafkaProducer 객체 생성 <키, 밸류>
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);

        //ProducerRecord 객체 생성
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, "hello world 6");

        // KafkaProducer 메세지 전송
        try {
            RecordMetadata recordMetadata = kafkaProducer.send(producerRecord).get();
            logger.info("partition : "+ recordMetadata.partition());
            logger.info("offset : " + recordMetadata.offset());
            logger.info("timestamp : " + recordMetadata.timestamp());
        } catch (ExecutionException e) { // 동기화작업이라
            e.printStackTrace();
        } catch (InterruptedException e) { // 스레드라서
            e.printStackTrace();
        } finally {
            kafkaProducer.close();
        }

        kafkaProducer.flush();
        kafkaProducer.close();
    }
}
