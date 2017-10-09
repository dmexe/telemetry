package me.dmexe.telemetry.kafka;

import io.opentracing.tag.IntTag;
import io.opentracing.tag.StringTag;

class Constants {
  static final String COMPONENT_NAME = "kafka";

  static final StringTag RECORD_OFFSET = new StringTag("kafka.offset");
  static final IntTag RECORD_PARTITION = new IntTag("kafka.partition");
  static final StringTag RECORD_KEY = new StringTag("kafka.key");
  static final IntTag RECORD_KEY_SIZE = new IntTag("kafka.key_size");
  static final IntTag RECORD_VALUE_SIZE = new IntTag("kafka.value_size");

  static final String ERROR_KIND_LOG_NAME = "error.kind";
  static final String ERROR_MESSAGE_LOG_NAME = "error.message";

  static final String MDC_TOPIC = "kafka:topic";
  static final String MDC_PARTITION = "kafka:partition";
  static final String MDC_OFFSET = "kafka:offset";
  static final String MDC_KEY = "kafka:key";
}
