name := "jvm-bloggers-twitter-client"

version := "1.0"
scalaVersion := "2.11.8"

enablePlugins(DockerPlugin)

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
    "org.antlr" % "ST4" % "4.0.8",
    "net.manub" %% "scalatest-embedded-kafka" % "0.7.1" % "test"
  )
}

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

imageNames in docker := Seq(
  ImageName(s"jakubdziworski/${name.value}:latest"),
  ImageName(s"jakubdziworski/${name.value}:${version.value}")
)
dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("java")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}
