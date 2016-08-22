name := "jvm-bloggers-twitter-client"

version := "1.0"


libraryDependencies ++= {
  val akkaV = "2.4.9"
  Seq(
    "com.typesafe.akka" %% "akka-http-core" % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaV,
    "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test",
    "org.twitter4j" % "twitter4j-core" % "4.0.4"
  )
}

scalaVersion := "2.11.8"
    