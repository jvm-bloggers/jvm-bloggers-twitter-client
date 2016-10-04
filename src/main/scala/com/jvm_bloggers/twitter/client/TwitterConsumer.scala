package com.jvm_bloggers.twitter.client

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorSystem, Props}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{GraphDSL, Sink, Source}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import odelay.Timer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}
import retry.Success
import spray.json._

import concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal


trait KafkaConf {
  private val config = ConfigFactory.defaultApplication()
  val kafkaHost = config.getString("kafka.adress")
  val kafkaPort = config.getString("kafka.port")
  val kafkaAdrress = s"$kafkaHost:$kafkaPort"
  val kafkaGroupId = config.getString("kafka.group")
  val topicUpdateStatus = "com.jvm_bloggers.twitter.status.update"
  val topicRetweetStatus = "com.jvm_bloggers.twitter.status.retweet"

  def consumerSettings(implicit system: ActorSystem) = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers(kafkaAdrress)
    .withGroupId(kafkaGroupId)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def producerSettings(implicit system: ActorSystem) = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers(kafkaAdrress)
}

class TwitterConsumer extends LazyLogging with KafkaConf {

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private val StatusAlreadyPublishedId = -1L
  val twitterClient : TwitterClient = new TwitterClientImpl()

  val updateStatusSource = Consumer.committableSource(consumerSettings, Subscriptions.topics(topicUpdateStatus)).mapAsync(1) { msg =>
    logger.info(s"Received Update Status Message: $msg")
    val status = msg.record.value().parseJson.convertTo[UpdateStatusMessage]
    val updateStatusPromise = () => twitterClient.updateStatus(status).recoverWith[Long](failureHandler).map { result =>
      msg.committableOffset.commitScaladsl()
      result
    }
    retryForever(updateStatusPromise)
  }

  val retweetSource = Consumer.committableSource(consumerSettings, Subscriptions.topics(topicRetweetStatus)).mapAsync(1) { msg =>
    logger.info(s"Received Retweet Status Message: $msg")
    val rt = msg.record.value().parseJson.convertTo[ReTweetMessage]
    val retweetPromise = () => twitterClient.retweet(rt).recoverWith[Long](failureHandler).map { result =>
      msg.committableOffset.commitScaladsl()
      result
    }
    retryForever(retweetPromise)
  }

  def run() = {
    val loggingSink = Sink.foreach[Long](id => logger.info(s"Sucessfully handled tweet with id : $id"))
    updateStatusSource.merge(retweetSource).runWith(loggingSink)
  }

  private def failureHandler: PartialFunction[Throwable, Future[Long]] = {
    case StatusAlreadyExistsException(existingStatus) => {
      logger.warn(s"Status $existingStatus has already been published.")
      Future(StatusAlreadyPublishedId)
    }
    case ex: TwitterStatusRejectedException => {
      logger.error(s"Twitter update status (${ex.status}) failed. Details: ${ex.errorCode} : ${ex.errorMessage}")
      Future.failed(ex)
    }
  }
}