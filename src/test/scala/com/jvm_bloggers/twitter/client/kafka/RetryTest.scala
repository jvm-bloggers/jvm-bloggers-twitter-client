package com.jvm_bloggers.twitter.client.kafka

import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
/**
  * Created by kuba on 03.10.16.
  */
class RetryTest extends FunSuite with Matchers {

  test("should not retry if sucesfull") {
    val future = retryForever(() => Future(5))
    Await.result(future, 1 seconds) shouldBe (5)
  }

  test("should retry") {
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
