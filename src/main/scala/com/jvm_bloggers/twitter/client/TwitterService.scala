package com.jvm_bloggers.twitter.client

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol
import twitter4j.TwitterException

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val tweetFormat = jsonFormat1(UpdateStatusDTO.apply)
  implicit val reTweetFormat = jsonFormat2(ReTweetDTO.apply)
}

object Routes extends JsonSupport {
  val routes = (handleExceptions(exceptionHandler) & logRequestResult("jvm-bloggers-twitter-client", Logging.InfoLevel)) {
    (pathPrefix("status") & post) {
      (path("update") & entity(as[UpdateStatusDTO])) {
        tweet => complete(TwitterClient.updateStatus(tweet))
      } ~
        (path("retweet") & entity(as[ReTweetDTO])) {
          tweet => complete(TwitterClient.retweet(tweet))
        }
    }
  }

  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: TwitterException => complete(InternalServerError -> s"there was a problem with twitter request ${e.getMessage}")
    case e => complete(InternalServerError -> s"${e.getMessage}")
  }
}

object TwitterService {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  val logger = Logging(system, getClass)
  val config = ConfigFactory.load()
  val adress = config.getString("http.adress")
  val port = config.getInt("http.port")

  def main(args: Array[String]): Unit = {
    Http().bindAndHandle(Routes.routes, adress, port)
    logger.info(s"Running on $adress:$port")
  }
}