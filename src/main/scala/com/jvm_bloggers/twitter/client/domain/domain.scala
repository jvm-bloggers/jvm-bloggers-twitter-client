package com.jvm_bloggers.twitter.client.domain

import spray.json.DefaultJsonProtocol

/**
  * Created by kuba on 10.10.16.
  */
case class UpdateStatusTweet(status: String)
object UpdateStatusTweet extends DefaultJsonProtocol {
  implicit val format = jsonFormat1(UpdateStatusTweet.apply)
}

case class ReTweet(comment: String, tweetId: Long)
object ReTweet extends DefaultJsonProtocol {
  implicit val format = jsonFormat2(ReTweet.apply)
}

