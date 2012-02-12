organization := "eu.regadas"

name := "jira-tt-soap"

version := "0.1-SNAPSHOT"

seq(lsSettings :_*)

seq(coffeeSettings: _*)


(resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (
  resourceManaged in Compile) {
    _ / "www" / "js"
}

libraryDependencies ++= Seq(
    "axis" % "axis" % "1.4",
    "joda-time" % "joda-time" % "2.0",
    "org.joda" % "joda-convert" % "1.2",
    "net.liftweb" %% "lift-json" % "2.4",
    "net.databinder" %% "unfiltered-netty-server" % "0.5.3",
    "net.databinder" %% "unfiltered-json" % "0.5.3",
    "com.weiglewilczek.slf4s" %% "slf4s" % "1.0.7",
    "org.slf4j" % "slf4j-log4j12" % "1.6.4",
    "org.slf4j" % "slf4j-api" % "1.6.4",
    "com.mongodb.casbah" %% "casbah" % "2.1.5-1"
)
