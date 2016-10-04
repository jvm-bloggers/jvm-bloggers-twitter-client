package com.jvm_bloggers.twitter.client

/**
  * Created by kuba on 01.10.16.
  */

case class StatusAlreadyExistsException(status: String) extends RuntimeException(s"status - '$status' already exists")
case class TwitterStatusRejectedException(status:String,errorCode: Int, errorMessage: String) extends RuntimeException(s"Twitter update status failed: $errorCode - $errorMessage")