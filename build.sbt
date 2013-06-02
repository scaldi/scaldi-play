name := "scaldi-play"

description := "Scaldi-Play - Scaldi integration for Play framework"

organization := "com.github.scaldi"

version := "0.3-SNAPSHOT"

crossScalaVersions := Seq("2.10.1")

scalaVersion := "2.10.1"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
    "play" %% "play" % "2.1.1",
    "com.github.scaldi" %% "scaldi" % "0.2",
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

pomExtra := <xml:group>
  <url>http://scaldi.github.io/scaldi/Scaldi.html</url>
  <inceptionYear>2013</inceptionYear>
  <licenses>
    <license>
      <name>Apache License, ASL Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <developers>
    <developer>
      <id>OlegIlyenko</id>
      <name>Oleg Ilyenko</name>
    </developer>
  </developers>
  <issueManagement>
    <system>GitHub</system>
    <url>http://github.com/scaldi/scaldi-play/issues</url>
  </issueManagement>
  <scm>
    <connection>scm:git:git@github.com:scaldi/scaldi-play.git</connection>
    <url>git@github.com:scaldi/scaldi-play.git</url>
  </scm>
</xml:group>

resolvers += "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/"