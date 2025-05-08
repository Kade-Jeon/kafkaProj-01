package com.example.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerMtopicRebalance {

    public static final Logger logger = LoggerFactory.getLogger(ConsumerMtopicRebalance.class);

    public static void main(String[] args) {

        //String topicName = "simple-topic";
        String topicName = "pizza-topic";

        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "group-assign");
        props.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());


        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(List.of("topic-p3-t1", "topic-p3-t2"));

        // main Thread 참조 변수
        Thread mainTread = Thread.currentThread();

        // main Thread 종료 시, 별도의 thread로 kafkaconsumer wakeup 메서드 호출하도록 함.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.info("Main program starts to exit by calling wakeup");
                kafkaConsumer.wakeup();

                //메인 스레드가 죽을때까지 버티고 같이 죽어야 한다.
                try {
                    mainTread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });

        try {
            while (true) {
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));// 가져올 데이터가 없다면! 최대 1초동안 기다림. 만약 있다면 바로 가져옴
                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("[CONSUMER] Topic: {}, Key: {},  Partition: {}, Offset: {}, Value: {},",
                            record.topic() ,record.key(), record.partition(), record.offset(), record.value());
                }
            }
        } catch (WakeupException e) {
            logger.error("WakeupException has been caught.");
        } finally {
            kafkaConsumer.close();
        }
    }
}
