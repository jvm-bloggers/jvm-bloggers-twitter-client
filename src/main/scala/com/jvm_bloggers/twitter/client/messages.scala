package com.jvm_bloggers.twitter.client

import spray.json.DefaultJsonProtocol

/**
  * Created by kuba on 25.08.16.
  */
trait TwittertMessage
case class UpdateStatusMessage(status: String) extends TwittertMessage
case class ReTweetMessage(comment: String, tweetId: Long) extends TwittertMessage
object ReTweetMessage extends DefaultJsonProtocol {
  implicit val reTweetFormat = jsonFormat2(ReTweetMessage.apply)
}
object UpdateStatusMessage extends DefaultJsonProtocol {
  implicit val tweetFormat = jsonFormat1(UpdateStatusMessage.apply)
}
