package com.jvm_bloggers.twitter.client

import odelay.Timer
import retry.Success

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by kuba on 02.10.16.
  */
package object kafka {
  def retryForever[T](promise: () => Future[T],interval:FiniteDuration = 5.seconds)(implicit executionContext: ExecutionContext) = {
    implicit val sucessDefinition = Success[T](t => true)
    retry.Pause.forever(interval)(Timer.default)(promise)
  }
}
