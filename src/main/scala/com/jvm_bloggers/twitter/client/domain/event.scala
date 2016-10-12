package com.jvm_bloggers.twitter.client.domain

import spray.json.DefaultJsonProtocol

/**
  * Created by kuba on 25.08.16.
  */
trait JvmBloggersEvent

case class NewIssuePublished(issueNumber:Long,url: String) extends JvmBloggersEvent  {
  override def toString: String = issueNumber.toString
}

object NewIssuePublished extends DefaultJsonProtocol {
  implicit val format = jsonFormat2(NewIssuePublished.apply)
}
