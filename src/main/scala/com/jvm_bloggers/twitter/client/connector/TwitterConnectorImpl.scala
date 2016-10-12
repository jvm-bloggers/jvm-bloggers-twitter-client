package com.jvm_bloggers.twitter.client.connector

import com.jvm_bloggers.twitter.client.domain._
import com.typesafe.config.ConfigFactory
import twitter4j.conf.ConfigurationBuilder
import twitter4j.{Status, Twitter, TwitterException, TwitterFactory}
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future


class TwitterConnectorImpl extends TwitterConnector {
  private val config = ConfigFactory.load()

  override def updateStatus(status: UpdateStatusTweet): Future[Long] = getClient.flatMap(client => updateStatus(status.status,client))

  override def retweet(tweetDto: ReTweet): Future[Long] = {
    getClient.flatMap { client =>
      val status = client.showStatus(tweetDto.tweetId)
      val userName = status.getUser.getName
      val tweetId = status.getId
      val comment = tweetDto.comment
      updateStatus(s"$comment https://twitter.com/$userName/status/$tweetId",client)
    }
  }

  private def updateStatus(status: String,client: Twitter) : Future[Long] = {
    def getStatusId: (Status) => String = _.getId.toString
    Future(client.updateStatus(status)).recoverWith {
      case e:TwitterException if e.getErrorCode == 187 => Future.failed(StatusAlreadyExistsException(status))
      case e:TwitterException => Future.failed(TwitterStatusRejectedException(status,e.getErrorCode,e.getErrorMessage))
    }.map(_.getId)
  }

  private def getClient: Future[Twitter] = {
    val consumerKey = config.getString("twitter.credentials.consumerKey")
    val consumerSecret = config.getString("twitter.credentials.consumerSecret")
    val accessToken = config.getString("twitter.credentials.accessToken")
    val accessSecret = config.getString("twitter.credentials.accessSecret")

    val cb = new ConfigurationBuilder()
    cb.setDebugEnabled(false)
      .setOAuthConsumerKey(consumerKey)
      .setOAuthConsumerSecret(consumerSecret)
      .setOAuthAccessToken(accessToken)
      .setOAuthAccessTokenSecret(accessSecret)
    val factory = new TwitterFactory(cb.build())
    Future(factory.getInstance())
  }
}

