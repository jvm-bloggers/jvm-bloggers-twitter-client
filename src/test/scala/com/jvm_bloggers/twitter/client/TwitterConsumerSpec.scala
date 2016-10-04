package com.jvm_bloggers.twitter.client

import akka.actor.ActorSystem
import akka.kafka.ProducerMessage.Message
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import akka.{Done, NotUsed}
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by kuba on 29.09.16.
  */
class TwitterConsumerSpec extends TestKit(ActorSystem("IntegrationSpec"))
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with KafkaConf {

  implicit val materializer = ActorMaterializer()(system)
  implicit val executionContext = system.dispatcher
  implicit val embeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort.toInt, 2181)
  val random = scala.util.Random
  val Timeout = 15 seconds
  val TweetIdRegex = "[0-9]+"
  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers(kafkaAdrress)

  def randomStatus() = "Update status integration test : " + random.alphanumeric.take(100).mkString

  def randomRTComment() = "Retweet integration test: : " + random.alphanumeric.take(35).mkString

  override def afterAll(): Unit = {
    shutdown(system, 30.seconds)
    EmbeddedKafka.stop()
    super.afterAll()
  }

  def produceMessage(topic: String, msg: String): Future[Done] = {
    Source.single[String](msg)
      .map { m =>
        val record = new ProducerRecord[Array[Byte], String](topic, m)
        Message(record, NotUsed)
      }
      .viaMat(Producer.flow(producerSettings))(Keep.right)
      .runWith(Sink.ignore)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    EmbeddedKafka.start()
  }

  "should post status" in {
    val msg = UpdateStatusMessage(randomStatus()).toJson.toString()
    Await.result(produceMessage(topicUpdateStatus, msg), remainingOrDefault)
    val consumer = new TwitterConsumer()
    val probe = consumer.updateStatusSource.runWith(TestSink.probe)
    probe.request(1)
    probe.expectNext(Timeout).toString should fullyMatch regex TweetIdRegex
    probe.cancel()
  }

  "Posting same status twice should be handled gracefully" in {
    val msg = UpdateStatusMessage(randomStatus()).toJson.toString()
    Await.result(produceMessage(topicUpdateStatus, msg), remainingOrDefault)
    Await.result(produceMessage(topicUpdateStatus, msg), remainingOrDefault)
    val consumer = new TwitterConsumer()
    val probe = consumer.updateStatusSource.runWith(TestSink.probe)
    probe.request(2)
    probe.expectNext(d = Timeout).toString should fullyMatch regex TweetIdRegex
    probe.expectNext(d = Timeout) shouldBe (-1L)
    probe.cancel()
  }

  "Should retweet status" in {
    val msg = ReTweetMessage(randomRTComment(), 768510460845035520L).toJson.toString()
    Await.result(produceMessage(topicRetweetStatus, msg), remainingOrDefault)
    val consumer = new TwitterConsumer()
    val probe = consumer.retweetSource.runWith(TestSink.probe)
    probe.request(1)
    probe.expectNext(d = Timeout).toString should fullyMatch regex TweetIdRegex
    probe.cancel()
  }

  "Retweeting same status twice should be handled gracefully" in {
    val msg = ReTweetMessage(randomRTComment(), 768510460845035520L).toJson.toString()
    Await.result(produceMessage(topicRetweetStatus, msg), remainingOrDefault)
    Await.result(produceMessage(topicRetweetStatus, msg), remainingOrDefault)
    val consumer = new TwitterConsumer()
    val probe = consumer.retweetSource.runWith(TestSink.probe)
    probe.request(2)
    probe.expectNext(d = Timeout).toString should fullyMatch regex TweetIdRegex
    probe.expectNext(d = Timeout) shouldBe (-1L)
    probe.cancel()
  }


  "Should retry update status when client failed until sucesfull" in {
    class MockedConsumer extends TwitterConsumer {
        override val twitterClient = new TwitterClient {
        var counter = 0

        override def updateStatus(updateStatusMessage: UpdateStatusMessage): Future[Long] = {
          if(counter < 2) {
            counter += 1
            Future.failed(new RuntimeException())
          }
          Future(15L)
        }
        override def retweet(reTweetMessage: ReTweetMessage): Future[Long] = ???
      }
    }

    val msg = UpdateStatusMessage(randomStatus()).toJson.toString()
    Await.result(produceMessage(topicUpdateStatus, msg), remainingOrDefault)
    val thirdTrySucessConsumer = new MockedConsumer()
    val probe = thirdTrySucessConsumer.updateStatusSource.runWith(TestSink.probe)
    probe.request(1)
    probe.expectNext(Timeout, 15L)
    probe.cancel()
  }

  "Should retry retweet when client failed until sucesfull" in {
    class MockedConsumer extends TwitterConsumer {
      override val twitterClient = new TwitterClient {
        var counter = 0

        override def updateStatus(updateStatusMessage: UpdateStatusMessage): Future[Long] = ???

        override def retweet(reTweetMessage: ReTweetMessage): Future[Long] = {
          if (counter < 2) {
            counter += 1
            Future.failed(new RuntimeException())
          }
          Future(15L)
        }
      }
    }
    val msg = ReTweetMessage(randomRTComment(),768510460845035520L).toJson.toString()
    Await.result(produceMessage(topicRetweetStatus, msg), remainingOrDefault)
    val thirdTrySucessConsumer = new MockedConsumer()
    val probe = thirdTrySucessConsumer.retweetSource.runWith(TestSink.probe)
    probe.request(1)
    probe.expectNext(Timeout, 15L)
    probe.cancel()
  }
}
