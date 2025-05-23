package com.practice.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.practice.kafka.model.OrderModel;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderSerializer implements Serializer<OrderModel> {
    public static final Logger logger = LoggerFactory.getLogger(OrderSerializer.class);

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Override
    public byte[] serialize(String topic, OrderModel orderModel) {
        byte[] serialziedOrder = null;
        try {
            serialziedOrder = objectMapper.writeValueAsBytes(orderModel);
        } catch (JsonProcessingException e) {
            logger.error("JSON processing exception : " + e.getMessage());
        }
        return serialziedOrder;
    }
}
