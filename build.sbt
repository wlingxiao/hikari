name := "hikari"

version := "0.1"

scalaVersion := "2.12.4"

val akkaHttpVersion = "10.1.0-RC1"

val akkaVersion = "2.5.8"

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

libraryDependencies ++= Seq(
  "io.netty" % "netty-all" % "4.1.21.Final",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
)
