name := "concord"
organization := "eu.coompany"
version := "0.0.1"
scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.21",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "com.typesafe" % "config" % "1.3.0",
    "com.typesafe.akka" %% "akka-actor" % "2.4.8",
    "com.typesafe.akka" %% "akka-remote" % "2.4.8",

    "com.typesafe.akka" %% "akka-testkit" % "2.4.8" % "test",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

enablePlugins(JavaAppPackaging)
