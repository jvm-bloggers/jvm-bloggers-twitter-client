package com.jvm_bloggers.twitter.client.template

import com.jvm_bloggers.twitter.client.domain.NewIssuePublished
import com.typesafe.config.ConfigFactory
import org.codehaus.jackson.map.SerializerFactory.Config
import org.scalatest.{FunSuite, Matchers}

/**
  * Created by kuba on 09.10.16.
  */
class NewIssueTemplateTest extends FunSuite with Matchers {

  def getTemplate(index: Int): String = {
    ConfigFactory.defaultApplication().getStringList("newIssue.templates").get(index)
  }

  test("should get template") {
    val template = getTemplate(0)
    val actual = new TemplateResolverImpl().resolve(NewIssuePublished(15, "jvmbloggers/15"), template).status
    val expected = "Nowe, już 15 wydanie JVM Bloggers jest dostępne na stronie jvmbloggers/15"
    actual shouldBe expected
  }

}
