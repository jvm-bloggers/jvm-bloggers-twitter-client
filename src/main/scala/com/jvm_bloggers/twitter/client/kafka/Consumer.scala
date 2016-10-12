package com.jvm_bloggers.twitter.client.kafka

import akka.actor.ActorSystem
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.jvm_bloggers.twitter.client.domain.{NewIssuePublished, ReTweet, StatusAlreadyExistsException, TwitterStatusRejectedException}
import com.jvm_bloggers.twitter.client.template.{FileTemplateProvider, TemplateProvider, TemplateResolver, TemplateResolverImpl}
import com.jvm_bloggers.twitter.client.connector.{TwitterConnector, TwitterConnectorImpl}
import com.typesafe.scalalogging.LazyLogging
import com.jvm_bloggers.twitter.client._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class Consumer extends LazyLogging with Configuration {

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private val StatusAlreadyPublishedId = -1L
  val twitterClient: TwitterConnector = new TwitterConnectorImpl()
  val templateResolver: TemplateResolver = new TemplateResolverImpl()
  val templateProvider: TemplateProvider = new FileTemplateProvider()

  val newIssueSource = Consumer.committableSource(consumerSettings, Subscriptions.topics(topicNewIssueReleased)).mapAsync(1) { msg =>
    logger.info(s"Received Update Status Message: $msg")
    val issue = msg.record.value().parseJson.convertTo[NewIssuePublished]
    val tweetTemplate = templateProvider.getRandomNewIssueStatusTemplate
    val status = templateResolver.resolve(issue, tweetTemplate)
    val updateStatusPromise = () => twitterClient.updateStatus(status).recoverWith[Long](failureHandler).map { result =>
      msg.committableOffset.commitScaladsl()
      result
    }
    retryForever(updateStatusPromise)
  }

  val retweetSource = Consumer.committableSource(consumerSettings, Subscriptions.topics(topicRetweetStatus)).mapAsync(1) { msg =>
    logger.info(s"Received Retweet Status Message: $msg")
    val rt = msg.record.value().parseJson.convertTo[ReTweet]
    val retweetPromise = () => twitterClient.retweet(rt).recoverWith[Long](failureHandler).map { result =>
      msg.committableOffset.commitScaladsl()
      result
    }
    retryForever(retweetPromise)
  }

  def run() = {
    val loggingSink = Sink.foreach[Long](id => logger.info(s"Sucessfully handled tweet with id : $id"))
    newIssueSource.merge(retweetSource).runWith(loggingSink)
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