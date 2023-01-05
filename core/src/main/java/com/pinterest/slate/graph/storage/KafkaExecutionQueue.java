package com.pinterest.slate.graph.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration2.Configuration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.google.common.collect.ImmutableList;
import com.pinterest.slate.graph.AbstractGraphExecutionQueue;

public class KafkaExecutionQueue extends AbstractGraphExecutionQueue {

  private KafkaConsumer<String, String> consumer;
  private KafkaProducer<String, String> producer;
  private String topic;

  public void init(Configuration config) throws IOException {
    topic = config.getString("topic");
    Properties consumerProps = new Properties();
    String bootstrapServer = String.join(",",
        Files.readAllLines(new File(config.getString("serverset")).toPath()));
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
    consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class.getName());
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class.getName());
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, config.getString("groupid"));
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumer = new KafkaConsumer<>(consumerProps);
    consumer.subscribe(ImmutableList.of(topic));

    Properties producerProps = new Properties();
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
    producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class.getName());
    producerProps.put(ProducerConfig.ACKS_CONFIG, "-1");
    producer = new KafkaProducer<>(producerProps);
  }

  public String take() {
    ConsumerRecords<String, String> poll = consumer.poll(Duration.ofSeconds(10));
    for (ConsumerRecord<String, String> record : poll) {
      consumer.commitSync();
      return record.value();
    }
    return null;
  }

  @Override
  public boolean isEmpty() {
    throw new UnsupportedOperationException();
  }

  public void add(String executionId) throws IOException {
    try {
      producer.send(new ProducerRecord<String, String>(topic, executionId, executionId)).get();
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public void delete(String executionId) throws IOException {
    try {
      producer.send(new ProducerRecord<String, String>(topic, executionId, null));
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void bootstrap(List<String> graphs) {
  }

}
