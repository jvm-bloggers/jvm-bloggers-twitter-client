package com.jvm_bloggers.twitter.client

import odelay.Timer
import org.scalatest.{FunSuite, Matchers}
import retry.Success

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import concurrent.ExecutionContext.Implicits.global
/**
  * Created by kuba on 03.10.16.
  */
class RetryTest extends FunSuite with Matchers {

  test("should not retry if sucesfull") {
    val future = retryForever(() => Future(5))
    Await.result(future, 1 seconds) shouldBe (5)
  }

  test("should retry once") {
    var tries = 0
    val eventualLong = () => Future {
      tries += 1
      if (tries < 2) Future.failed(new RuntimeException())
      3L
    }
    val future = retryForever(eventualLong, 1 seconds)
    Await.result(future, 2 seconds) shouldBe (3)
  }
}
