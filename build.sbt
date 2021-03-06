organization := "org.hikari"
name := "hikari"
version := "0.0.1-SNAPSHOT"
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

val SwaggerVersion = "1.5.18"

val NettyVersion = "4.1.21.Final"

libraryDependencies ++= Seq(
  "io.netty" % "netty-codec-http" % NettyVersion,
  "io.netty" % "netty-handler" % NettyVersion,
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0",

  // log
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.slf4j" % "slf4j-api" % "1.7.25",

  // test
  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
  "org.mockito" % "mockito-core" % "2.15.0" % Test,

  // json support
  "com.fasterxml.jackson.core" % "jackson-core" % JacksonVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonVersion,

  // swagger
  "io.swagger" % "swagger-core" % SwaggerVersion,
  "io.swagger" % "swagger-annotations" % SwaggerVersion,
  "io.swagger" % "swagger-models" % SwaggerVersion,
  "org.webjars" % "swagger-ui" % "2.2.10-1",

  // config
  "com.typesafe" % "config" % "1.3.1"
)
