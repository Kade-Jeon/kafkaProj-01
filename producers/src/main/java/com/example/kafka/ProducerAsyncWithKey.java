package com.example.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerAsyncWithKey {
    public static final Logger logger = LoggerFactory.getLogger(ProducerAsyncWithKey.class);

    public static void main(String[] args) {
        String topicName = "multipart-topic";

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

        for (int seq = 0; seq < 20; seq++) {
            //ProducerRecord 객체 생성
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, String.valueOf(seq),"hello world" + seq);

            // KafkaProducer 메세지 전송
            kafkaProducer.send(producerRecord, (metadata, exception) -> {
                if (exception == null) {
                    logger.info("partition : " + metadata.partition());
                    logger.info("offset : " + metadata.offset());
                    logger.info("timestamp : " + metadata.timestamp());
                } else {
                    logger.error("exception error from broker / " + exception.getMessage());
                }
            });
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        kafkaProducer.close();
    }
}
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