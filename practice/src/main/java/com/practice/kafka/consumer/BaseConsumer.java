package com.practice.kafka.consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class BaseConsumer<K extends Serializable, V extends Serializable> {
    public static final Logger logger = LoggerFactory.getLogger(BaseConsumer.class.getName());

    private KafkaConsumer<K, V> kafkaConsumer;
    private List<String> topics;

    public BaseConsumer(Properties consumerProps, List<String> topics) {
        this.kafkaConsumer = new KafkaConsumer<>(consumerProps);
        this.topics = topics;
    }

    public void initConsumer() {
        this.kafkaConsumer.subscribe(this.topics);
        shutdownHookToRuntime(this.kafkaConsumer);
    }

    private void shutdownHookToRuntime(KafkaConsumer<K, V> kafkaConsumer) {
        //main thread
        Thread mainThread = Thread.currentThread();

        //main thread 종료시 별도의 thread로 KafkaConsumer wakeup()메소드를 호출하게 함.
        // new Thread() 에서는 객체의 필드를 참조할 수 없다. (this.kafkaConsumer가 안됨) 그러므로 파라미터로 넘겨준다. -> private void shutdownHookToRuntime(KafkaConsumer<K, V> kafkaConsumer)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.info(" main program starts to exit by calling wakeup");
                kafkaConsumer.wakeup();

                try {
                    mainThread.join(); // 메인스레드 종료를 기다렸다가, weakeup 메서드 호출하는 콜백스레드가 함께 종료됨.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void pollConsumes(long durationMillis, String commitMode) {
        try {
            while (true) {
                if (commitMode.equals("sync")) {
                    pollCommitSync(durationMillis);
                } else {
                    pollCommitAsync(durationMillis);
                }
            }
        } catch (WakeupException e) {
            logger.error("wakeup exception has been called");
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            logger.info("##### commit sync before closing");
            kafkaConsumer.commitSync();
            logger.info("finally consumer is closing");
            closeConsumer();
        }
    }


    private void pollCommitSync(long durationMillis) throws WakeupException, Exception {
        ConsumerRecords<K, V> consumerRecords = this.kafkaConsumer.poll(Duration.ofMillis(durationMillis));
        processRecords(consumerRecords);
        try {
            if (consumerRecords.count() > 0) {
                this.kafkaConsumer.commitSync();
                logger.info("commit sync has been called");
            }
        } catch (CommitFailedException e) {
            logger.error(e.getMessage());
        }
    }

    private void pollCommitAsync(long durationMillis) throws WakeupException, Exception {
        ConsumerRecords<K, V> consumerRecords = this.kafkaConsumer.poll(durationMillis);
        processRecords(consumerRecords);

        this.kafkaConsumer.commitAsync((offsets, exception) -> {
            if (exception != null) {
                logger.error("offsets {} is not committed, error {}", offsets, exception.toString());
            }
        });
    }

    private void processRecords(ConsumerRecords<K, V> records) {
        records.forEach(this::processRecord);
    }

    private void processRecord(ConsumerRecord<K, V> record) {
        logger.info("record key: {}. record.partition: {}, record.offset: {}, record.value: {}",
                record.key(), record.partition(), record.offset(), record.value());
    }

    public void closeConsumer() {
        this.kafkaConsumer.close();
    }

    public static void main(String[] args) {
        String topicName = "file-topic";

        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "file-group");
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); //수동커밋 하겠다.

        BaseConsumer<String, String> baseConsumer = new BaseConsumer<String, String>(props, List.of(topicName));
        baseConsumer.initConsumer();
        String commitMode = "async";

        baseConsumer.pollConsumes(100, commitMode);
        baseConsumer.closeConsumer();

    }


}