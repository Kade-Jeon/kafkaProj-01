package com.practice.kafka.producer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileProducer {

    public static final Logger logger = LoggerFactory.getLogger(FileProducer.class);

    public static void main(String[] args) {
        String topicName = "file-topic";
        String filePath = "C:\\Users\\crinity\\Documents\\study\\kafka-core\\kafkaProj-01\\practice\\src\\main\\resources\\pizza_sample.txt"; //window

        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // KafkaProducer 객체 생성 <키, 밸류>
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);

        // kafkaProducer 객체 생성 -> ProducerRecord 생성 -> send() 비동기 방식 전송
        sendFileMessages(kafkaProducer, topicName, filePath);

        kafkaProducer.close();
    }

    private static void sendFileMessages(KafkaProducer<String, String> kafkaProducer, String topicName,
                                         String filePath) {
        try {
            FileReader fileReader = new FileReader(filePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = "";
            final String delimiter = ",";

            while ((line = bufferedReader.readLine()) != null) {
                String[] tokens = line.split(delimiter);
                String key = tokens[0];
                StringBuffer value = new StringBuffer();

                for (int i = 1; i < tokens.length; i++) {
                    if (i != tokens.length - 1) {
                        value.append(tokens[i] + delimiter);
                    } else {
                        value.append(tokens[i]);
                    }
                }

                sendMessage(kafkaProducer, topicName, key, value.toString());
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private static void sendMessage(KafkaProducer<String, String> kafkaProducer, String topicName, String key, String value) {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, key,value);
        logger.info("key: " + key + " value: " + value);

        // KafkaProducer 메세지 전송
        kafkaProducer.send(producerRecord, (metadata, exception) -> {
            if (exception == null) {
                logger.info("""
                        ##### record metadata received #####
                        partition: {}
                        offset: {}
                        timestamp: {}
                        """,
                        metadata.partition(),metadata.offset(),metadata.timestamp());

            } else {
                logger.error("exception error from broker / " + exception.getMessage());
            }
        });
    }
}
