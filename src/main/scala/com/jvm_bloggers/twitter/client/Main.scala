package com.jvm_bloggers.twitter.client

import com.jvm_bloggers.twitter.client.kafka.Consumer

/**
  * Created by kuba on 30.09.16.
  */
object Main extends App {
  new Consumer().run()
}
