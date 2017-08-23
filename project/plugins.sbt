credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

val munerisRelRepo = "Muneris Releases Repository" at "https://team-repository.muneris.io/nexus/repository/public"

val munerisSnapRepo = "Muneris Snapshots Repository" at "https://team-repository.muneris.io/nexus/repository/public-snapshots"

resolvers ++= Seq(munerisRelRepo, munerisSnapRepo)

externalResolvers := Resolver.withDefaultResolvers(resolvers.value, mavenCentral = false)
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.6.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")