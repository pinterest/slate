package com.pinterest.slate.graph.audit;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

import org.apache.commons.configuration2.Configuration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.google.gson.Gson;
import com.pinterest.slate.graph.AbstractGraphAuditSink;
import com.pinterest.slate.graph.ExecutionGraph;

public class KafkaAuditSink extends AbstractGraphAuditSink {

  private static final Gson GSON = new Gson();
  private String topic;
  private KafkaProducer<String, String> producer;

  @Override
  public void init(Configuration config) throws Exception {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        String.join(",", Files.readAllLines(new File(config.getString("serverset")).toPath())));
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class.getCanonicalName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class.getCanonicalName());
    topic = config.getString("topic");
    producer = new KafkaProducer<>(props);
  }

  @Override
  public void audit(ExecutionGraph executionGraph) throws Exception {
    producer.send(new ProducerRecord<String, String>(topic, GSON.toJson(executionGraph)));
    producer.flush();
  }

}
