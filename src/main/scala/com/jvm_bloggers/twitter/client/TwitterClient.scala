package com.jvm_bloggers.twitter.client


import scala.concurrent.Future

/**
  * Created by kuba on 14.08.16.
  */

trait TwitterClient {
  def updateStatus(updateStatusMessage: UpdateStatusMessage) : Future[Long]
  def retweet(reTweetMessage: ReTweetMessage) : Future[Long]
}
