

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com")

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.1")

addSbtPlugin("me.lessis" % "coffeescripted-sbt" % "0.2.1")
