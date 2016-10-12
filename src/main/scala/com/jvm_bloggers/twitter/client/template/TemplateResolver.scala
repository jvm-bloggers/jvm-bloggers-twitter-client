package com.jvm_bloggers.twitter.client.template

import com.jvm_bloggers.twitter.client.domain.{NewIssuePublished, UpdateStatusTweet}
import org.stringtemplate.v4.ST

/**
  * Created by kuba on 09.10.16.
  */


trait TemplateResolver {
  def resolve(newIssuePublished: NewIssuePublished, template: String) : UpdateStatusTweet
}




