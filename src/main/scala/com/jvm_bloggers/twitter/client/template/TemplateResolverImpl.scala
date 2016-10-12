package com.jvm_bloggers.twitter.client.template

import com.jvm_bloggers.twitter.client.domain.{NewIssuePublished, UpdateStatusTweet}
import org.stringtemplate.v4.ST

/**
  * Created by kuba on 12.10.16.
  */
class TemplateResolverImpl extends TemplateResolver{
  override def resolve(newIssuePublished: NewIssuePublished, template: String): UpdateStatusTweet = {
    val st = new ST(template)
    st.add("issueNumber",newIssuePublished.issueNumber)
    st.add("url",newIssuePublished.url)
    UpdateStatusTweet(st.render())
  }
}
