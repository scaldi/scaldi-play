name := "scaldi-play"

description := "Scaldi-Play - Scaldi integration for Play framework"

organization := "org.scaldi"

version := "0.4-SNAPSHOT"

scalaVersion := "2.10.3"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.2.2" % "provided",
  "org.scaldi" %% "scaldi" % "0.3"
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