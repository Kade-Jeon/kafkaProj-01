package com.example.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerPartitionAssignSeek {

    public static final Logger logger = LoggerFactory.getLogger(ConsumerPartitionAssignSeek.class);

    public static void main(String[] args) {

        //String topicName = "simple-topic";
        String topicName = "pizza-topic";

        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "group_pizza_assign_seek_v001");
        //props.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "6000");
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(props);
        TopicPartition topicPartition = new TopicPartition(topicName, 0);
        //kafkaConsumer.subscribe(List.of(topicName));
        kafkaConsumer.assign(List.of(topicPartition));
        kafkaConsumer.seek(topicPartition, 5L);

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

        //pollAutoCommit(kafkaConsumer);
        //pollSyncCommit(kafkaConsumer);
        //pollAsyncCommit(kafkaConsumer);
        pollNoCommit(kafkaConsumer);
    }

    private static void pollNoCommit(KafkaConsumer<String, String> kafkaConsumer) {
        int loopCnt = 0;
        try {
            while (true) { // 보통 여기 ~까지 읽어오도록 세팅함.
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(
                        Duration.ofMillis(1000));// 가져올 데이터가 없다면! 최대 1초동안 기다림. 만약 있다면 바로 가져옴
                logger.info("##### loopCnt: {}, consumerRecords: {}", loopCnt++, consumerRecords.count());

                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("[CONSUMER] Key: {},  Partition: {}, Offset: {}, Value: {},",
                            record.key(), record.partition(), record.offset(), record.value());
                }
            }
        } catch (WakeupException e) {
            logger.error("WakeupException has been caught.");
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            kafkaConsumer.close();
        }
    }

    private static void pollAsyncCommit(KafkaConsumer<String, String> kafkaConsumer) {
        int loopCnt = 0;
        try {
            while (true) {
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(
                        Duration.ofMillis(1000));// 가져올 데이터가 없다면! 최대 1초동안 기다림. 만약 있다면 바로 가져옴
                logger.info("##### loopCnt: {}, consumerRecords: {}", loopCnt++, consumerRecords.count());

                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("[CONSUMER] Key: {},  Partition: {}, Offset: {}, Value: {},",
                            record.key(), record.partition(), record.offset(), record.value());
                }
                kafkaConsumer.commitAsync(new OffsetCommitCallback() {
                    @Override
                    public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
                        if (exception != null) {
                            logger.error("Offsets: {} is not committed, error: {}", offsets, exception.getMessage());
                        }
                    }
                });
            }
        } catch (WakeupException e) {
            logger.error("WakeupException has been caught.");
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            logger.info("##### CommitSync() is called before closing");
            kafkaConsumer.commitSync();
            logger.info("##### finally consumer is closing");
            kafkaConsumer.close();
        }
    }

    private static void pollSyncCommit(KafkaConsumer<String, String> kafkaConsumer) {
        int loopCnt = 0;
        try {
            while (true) {
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(
                        Duration.ofMillis(1000));// 가져올 데이터가 없다면! 최대 1초동안 기다림. 만약 있다면 바로 가져옴
                logger.info("##### loopCnt: {}, consumerRecords: {}", loopCnt++, consumerRecords.count());

                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("[CONSUMER] Key: {},  Partition: {}, Offset: {}, Value: {},",
                            record.key(), record.partition(), record.offset(), record.value());
                }
                try {
                    if (consumerRecords.count() > 0) {
                        kafkaConsumer.commitSync();
                        logger.info("commitSync() has been called");
                    }
                } catch (CommitFailedException e) { // commit 을 여러번 retry 하고 더이상 할 수 없을 때 해당 예외가 발생한다.
                    logger.error(e.getMessage());
                }

            }
        } catch (WakeupException e) {
            logger.error("WakeupException has been caught.");
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            kafkaConsumer.close();
        }
    }

    private static void pollAutoCommit(KafkaConsumer<String, String> kafkaConsumer) {
        int loopCnt = 0;
        try {
            while (true) {
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));// 가져올 데이터가 없다면! 최대 1초동안 기다림. 만약 있다면 바로 가져옴
                logger.info("##### loopCnt: {}, consumerRecords: {}", loopCnt++, consumerRecords.count());

                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("[CONSUMER] Key: {},  Partition: {}, Offset: {}, Value: {},",
                            record.key(), record.partition(), record.offset(), record.value());
                }

                try {
                    logger.info("main thread is sleeping {}ms during whilte loop", loopCnt * 10000);
                    Thread.sleep( 10000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (WakeupException e) {
            logger.error("WakeupException has been caught.");
        } finally {
            kafkaConsumer.close();
        }
    }
}
