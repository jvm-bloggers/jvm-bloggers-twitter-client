package com.jvm_bloggers.twitter.client.kafka

import akka.actor.ActorSystem
import akka.kafka.ProducerMessage.Message
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import akka.{Done, NotUsed}
import com.jvm_bloggers.twitter.client.connector.TwitterConnector
import com.jvm_bloggers.twitter.client.domain.{NewIssuePublished, ReTweet, UpdateStatusTweet}
import com.jvm_bloggers.twitter.client.template.TemplateProvider
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
class ConsumerSpec extends TestKit(ActorSystem("IntegrationSpec"))
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with Configuration {

  implicit val materializer = ActorMaterializer()(system)
  implicit val executionContext = system.dispatcher
  implicit val embeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort.toInt, 2181)
  val random = scala.util.Random
  val Timeout = 15 seconds
  val TweetIdRegex = "[0-9]+"
  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers(kafkaAdrress)

  def randomNewIssueMessage() = {
    val randomInt = random.nextInt()
    NewIssuePublished(randomInt,s"http://jvm-bloggers.com/issue/$randomInt").toJson.toString()
  }

  def randomRTComment() = "Retweet integration test: : " + random.alphanumeric.take(35).mkString


  override protected def afterAll(): Unit = {
    shutdown(system, 30.seconds)
  }

  override def afterEach(): Unit = {
    EmbeddedKafka.stop()
    super.afterEach()
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

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    EmbeddedKafka.start()
  }

  "should post status" in {
    val msg = randomNewIssueMessage()
    Await.result(produceMessage(topicNewIssueReleased, msg), remainingOrDefault)
    val consumer = new Consumer()
    val probe = consumer.newIssueSource.runWith(TestSink.probe)
    probe.request(1)
    probe.expectNext(Timeout).toString should fullyMatch regex TweetIdRegex
    probe.cancel()
  }

  "Posting same status twice should be handled gracefully" in {
    val msg = randomNewIssueMessage()
    Await.result(produceMessage(topicNewIssueReleased, msg), remainingOrDefault)
    Await.result(produceMessage(topicNewIssueReleased, msg), remainingOrDefault)
    class MockedConsumer extends Consumer{
      override val templateProvider: TemplateProvider = new TemplateProvider {
        override def getRandomNewIssueStatusTemplate: String = "constant template"
      }
    }
    val consumer = new MockedConsumer()
    val probe = consumer.newIssueSource.runWith(TestSink.probe)
    probe.request(2)
    probe.expectNext(d = Timeout).toString should fullyMatch regex TweetIdRegex
    probe.expectNext(d = Timeout) shouldBe (-1L)
    probe.cancel()
  }

  "Should retweet status" in {
    val msg = ReTweet(randomRTComment(), 768510460845035520L).toJson.toString()
    Await.result(produceMessage(topicRetweetStatus, msg), remainingOrDefault)
    val consumer = new Consumer()
    val probe = consumer.retweetSource.runWith(TestSink.probe)
    probe.request(1)
    probe.expectNext(d = Timeout).toString should fullyMatch regex TweetIdRegex
    probe.cancel()
  }

  "Retweeting same status twice should be handled gracefully" in {
    val msg = ReTweet(randomRTComment(), 768510460845035520L).toJson.toString()
    Await.result(produceMessage(topicRetweetStatus, msg), remainingOrDefault)
    Await.result(produceMessage(topicRetweetStatus, msg), remainingOrDefault)
    val consumer = new Consumer()
    val probe = consumer.retweetSource.runWith(TestSink.probe)
    probe.request(2)
    probe.expectNext(d = Timeout).toString should fullyMatch regex TweetIdRegex
    probe.expectNext(d = Timeout) shouldBe (-1L)
    probe.cancel()
  }


  "Should retry update status when client failed until sucesfull" in {
    class MockedConsumer extends Consumer {
        override val twitterClient = new TwitterConnector {
        var counter = 0

        override def updateStatus(updateStatusMessage: UpdateStatusTweet): Future[Long] = {
          if(counter < 2) {
            counter += 1
            Future.failed(new RuntimeException())
          }
          Future(15L)
        }
        override def retweet(reTweetMessage: ReTweet): Future[Long] = ???
      }
    }

    Await.result(produceMessage(topicNewIssueReleased, randomNewIssueMessage()), remainingOrDefault)
    val consumer = new MockedConsumer()
    val probe = consumer.newIssueSource.runWith(TestSink.probe)
    probe.request(1)
    probe.expectNext(Timeout, 15L)
    probe.cancel()
  }

  "Should retry retweet when client failed until sucesfull" in {
    class MockedConsumer extends Consumer {
      override val twitterClient = new TwitterConnector {
        var counter = 0

        override def updateStatus(updateStatusMessage: UpdateStatusTweet): Future[Long] = ???

        override def retweet(reTweetMessage: ReTweet): Future[Long] = {
          if (counter < 2) {
            counter += 1
            Future.failed(new RuntimeException())
          }
          Future(15L)
        }
      }
    }
    val msg = ReTweet(randomRTComment(),768510460845035520L).toJson.toString()
    Await.result(produceMessage(topicRetweetStatus, msg), remainingOrDefault)
    val thirdTrySucessConsumer = new MockedConsumer()
    val probe = thirdTrySucessConsumer.retweetSource.runWith(TestSink.probe)
    probe.request(1)
    probe.expectNext(Timeout, 15L)
    probe.cancel()
  }
}
