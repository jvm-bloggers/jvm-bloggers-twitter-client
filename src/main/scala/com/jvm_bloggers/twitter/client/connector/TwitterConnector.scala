package com.jvm_bloggers.twitter.client.connector

import com.jvm_bloggers.twitter.client.domain.{ReTweet, UpdateStatusTweet}

import scala.concurrent.Future

/**
  * Created by kuba on 14.08.16.
  */

trait TwitterConnector {
  def updateStatus(updateStatusMessage: UpdateStatusTweet) : Future[Long]
  def retweet(reTweetMessage: ReTweet) : Future[Long]
}
