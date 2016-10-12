package com.jvm_bloggers.twitter.client.kafka

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

/**
  * Created by kuba on 10.10.16.
  */
trait Configuration {
  private val config = ConfigFactory.defaultApplication()
  val kafkaHost = config.getString("kafka.adress")
  val kafkaPort = config.getString("kafka.port")
  val kafkaAdrress = s"$kafkaHost:$kafkaPort"
  val kafkaGroupId = config.getString("kafka.group")
  val topicNewIssueReleased = config.getString("kafka.topics.issuePublished")
  val topicRetweetStatus = config.getString("kafka.topics.retweet")

  def consumerSettings(implicit system: ActorSystem) = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers(kafkaAdrress)
    .withGroupId(kafkaGroupId)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def producerSettings(implicit system: ActorSystem) = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers(kafkaAdrress)
}
