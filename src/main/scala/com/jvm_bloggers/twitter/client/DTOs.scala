package com.jvm_bloggers.twitter.client

/**
  * Created by kuba on 25.08.16.
  */
case class UpdateStatusDTO(status: String)
case class ReTweetDTO(comment: String, tweetId: Long)