package com.jvm_bloggers.twitter.client.template

import com.typesafe.config.ConfigFactory

import scala.util.Random

/**
  * Created by kuba on 12.10.16.
  */
class FileTemplateProvider extends TemplateProvider {
  private[FileTemplateProvider] val templates = ConfigFactory.defaultApplication().getStringList("newIssue.templates")
  override def getRandomNewIssueStatusTemplate: String = {
    templates.get(Random.nextInt(templates.size()))
  }
}
