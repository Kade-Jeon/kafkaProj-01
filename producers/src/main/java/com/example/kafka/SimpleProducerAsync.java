package com.example.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SimpleProducerAsync {
    public static final Logger logger = LoggerFactory.getLogger(SimpleProducerAsync.class);

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
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, "hello world 7");

        // KafkaProducer 메세지 전송
//        kafkaProducer.send(producerRecord, new Callback() {
//            @Override
//            public void onCompletion(RecordMetadata metadata, Exception exception) {
//                if (exception == null) {
//                    logger.info("partition : "+ metadata.partition());
//                    logger.info("offset : " + metadata.offset());
//                    logger.info("timestamp : " + metadata.timestamp());
//                } else {
//                    logger.error("exception error from broker / " + exception.getMessage());
//                }
//            }
//        });

        kafkaProducer.send(producerRecord, (metadata, exception)-> {
            if (exception == null) {
                logger.info("partition : "+ metadata.partition());
                logger.info("offset : " + metadata.offset());
                logger.info("timestamp : " + metadata.timestamp());
            } else {
                logger.error("exception error from broker / " + exception.getMessage());
            }
        });

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        kafkaProducer.close();
    }
}
