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
    "net.databinder" %% "unfiltered-json" % "0.5.3"
)
