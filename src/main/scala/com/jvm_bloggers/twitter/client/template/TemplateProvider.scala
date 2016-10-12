package com.jvm_bloggers.twitter.client.template

import com.typesafe.config.ConfigFactory

import scala.util.Random

/**
  * Created by kuba on 09.10.16.
  */
trait TemplateProvider {
  def getRandomNewIssueStatusTemplate: String
}

