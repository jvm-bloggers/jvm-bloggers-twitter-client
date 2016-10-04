name := "jvm-bloggers-twitter-client"

version := "1.0"


libraryDependencies ++= {
  val akkaV = "2.4.9"
  Seq(
    "com.typesafe.akka" %% "akka-http-core" % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaV,
    "com.typesafe.akka" %% "akka-stream-kafka" % "0.12",
    "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test",
    "org.twitter4j" % "twitter4j-core" % "4.0.4",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "me.lessis" %% "retry" % "0.2.0",
    "org.slf4j" % "log4j-over-slf4j" % "1.7.12" % "test",
    "net.manub" %% "scalatest-embedded-kafka" % "0.7.1" % "test"
  )
}

scalaVersion := "2.11.8"

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"
