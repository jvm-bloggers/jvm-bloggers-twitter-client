package com.jvm_bloggers.twitter.client

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration.DurationInt

/**
  * Created by kuba on 14.08.16.
  */
class ServiceIntegrationTest extends WordSpec with Matchers with ScalatestRouteTest {

  private val numericRegex = "[0-9]+"

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(new DurationInt(5).second)

  val random = scala.util.Random
  "should post status" in {
    val randomStatus = "Update status integration test : " + random.alphanumeric.take(100).mkString
    Post("/status/update", HttpEntity(`application/json`, s"""{ "status": "$randomStatus"}""")) ~> Routes.routes ~>
      check {
        status === Ok
        responseAs[String] should fullyMatch regex numericRegex
      }
  }

  "should retweet status" in {
    val randomComment = "Retweet integration test: " + random.alphanumeric.take(35).mkString
    Post("/status/retweet", HttpEntity(`application/json`, s"""{ "comment": "$randomComment","tweetId":768510460845035520}""")) ~> Routes.routes ~>
      check {
        status === Ok
        responseAs[String] should fullyMatch regex numericRegex
      }
  }

  "posting same status twice should fail" in {
    val randomStatus = "Same status twice integration test " + random.alphanumeric.take(100).mkString
    val post = Post("/status/update", HttpEntity(`application/json`, s"""{ "status": "$randomStatus"}"""))
    post ~> Routes.routes ~>
      check {
        responseAs[String] should fullyMatch regex numericRegex
      }
    post ~> Routes.routes ~>
      check {
        status === InternalServerError
        responseAs[String] should startWith("there was a problem with twitter request")
      }
  }
}
