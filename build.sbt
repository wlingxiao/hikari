name := "hikari"

version := "0.1"

scalaVersion := "2.12.4"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-unchecked"
)

parallelExecution in Test := false

resolvers += Classpaths.typesafeReleases

val JacksonVersion = "2.9.4"

libraryDependencies ++= Seq(
  "io.netty" % "netty-all" % "4.1.21.Final",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0",

  // log
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.slf4j" % "slf4j-api" % "1.7.25",

  // test
  "org.scalatest" %% "scalatest" % "3.0.4" % Test,

  // json support
  "com.fasterxml.jackson.core" % "jackson-core" % JacksonVersion,
  "org.json4s" %% "json4s-jackson" % "3.5.3"
)
