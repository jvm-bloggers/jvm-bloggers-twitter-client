package com.jvm_bloggers.twitter.client

import java.util.concurrent.Executors

import com.typesafe.config.ConfigFactory
import twitter4j.conf.ConfigurationBuilder
import twitter4j.{Twitter, TwitterFactory}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by kuba on 14.08.16.
  */
object TwitterClient {
  private val config = ConfigFactory.load()
  private val threadPoolSize = config.getInt("thread-pool-size")
  private implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threadPoolSize))

  def updateStatus(statusDto: UpdateStatusDTO): Future[String] = {
    getClient.map { client =>
      client.updateStatus(statusDto.status).getId.toString
    }
  }

  def retweet(tweetDto: ReTweetDTO): Future[String] = {
    getClient.map { client =>
      val status = client.showStatus(tweetDto.tweetId)
      val userName = status.getUser.getName
      val tweetId = status.getId
      val comment = tweetDto.comment
      client.updateStatus(s"$comment https://twitter.com/$userName/status/$tweetId").getId.toString
    }
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
