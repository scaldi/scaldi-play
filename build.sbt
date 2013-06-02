name := "scaldi-play"

organization := "com.github.scaldi"

version := "0.2"

crossScalaVersions := Seq("2.10.1")

scalaVersion := "2.10.1"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
    "play" %% "play" % "2.1.1",
    "com.github.scaldi" %% "scaldi" % "0.1.2",
    "org.scalatest" %% "scalatest" % "1.9.1" % "test"
)

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { x => false }

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

resolvers += "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/"