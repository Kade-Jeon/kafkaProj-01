package com.example.kafka;

import com.github.javafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class PizzaProducer {
    public static final Logger logger = LoggerFactory.getLogger(PizzaProducer.class);

    public static void sendPiazzaMessage (KafkaProducer<String, String> kafkaProducer,
                                          String topicName, int iterCount,
                                          int interIntervalMillis, int intervalMillis,
                                          int intervalCount, boolean sync) {
        PizzaMessage pizzaMessage = new PizzaMessage();
        int iterSeq = 0;
        long seed = 2022;
        Random random = new Random(seed);
        Faker faker = Faker.instance(random);

        while (iterSeq != iterCount) {
            HashMap<String, String> pMessage = pizzaMessage.produce_msg(faker, random, iterSeq);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName,
                    pMessage.get("key"), pMessage.get("message"));
            sendMessage(kafkaProducer, producerRecord, pMessage, sync);

            if((intervalCount > 0) && (iterSeq % intervalCount == 0)) {
                try {
                    logger.info("##### IntervalCount : {}, IntervalMillis : {} #####", intervalCount, intervalMillis);
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            }

            if (interIntervalMillis > 0 ) {
                try {
                    logger.info("##### InterIntervalMillis : {} #####", interIntervalMillis);
                    Thread.sleep(interIntervalMillis);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    public static void sendMessage (KafkaProducer<String, String> kafkaProducer,
                                    ProducerRecord<String, String> producerRecord,
                                    HashMap<String, String> pMessage, boolean sync) {
        if (!sync) {
            kafkaProducer.send(producerRecord, (metadata, exception) -> {
                if (exception == null) {
                    logger.info("async message : {}, partition : {}, offset : {}, timestamp : {}", pMessage.get("key"), metadata.partition(), metadata.offset(), metadata.timestamp());
                } else {
                    logger.error("exception error from broker / " + exception.getMessage());
                }
            });
        } else {
            try {
                RecordMetadata metadata = kafkaProducer.send(producerRecord).get();
                logger.info("partition : {}, offset : {}, timestamp : {}", metadata.partition(), metadata.offset(), metadata.timestamp());
            } catch (ExecutionException e) { // 동기화작업이라
                e.printStackTrace();
            } catch (InterruptedException e) { // 스레드라서
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        String topicName = "pizza-topic";
        //KafkaProducer Configuration Setting
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        //props.setProperty(ProducerConfig.ACKS_CONFIG, "0");
        //props.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, "32000");
        //props.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");

        // KafkaProducer 객체 생성 <키, 밸류>
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);

        sendPiazzaMessage(kafkaProducer, topicName, -1, 100,
                1000, 100, true);

        kafkaProducer.close();
    }
}