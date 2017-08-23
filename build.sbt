name := "scaldi-play"
organization := "org.scaldi"
version := "0.5.16"

description := "Scaldi-Play - Scaldi integration for Play framework"
homepage := Some(url("http://scaldi.org"))
licenses := Seq("Apache License, ASL Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

scalaVersion := "2.12.3"
scalacOptions ++= Seq("-deprecation", "-feature")
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
testOptions in Test += Tests.Argument("-oDF")

val playVersion = "2.6.3"
val slickVersion = "3.0.1"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % playVersion % "provided",
  "com.typesafe.play" %% "play-guice" % playVersion % "provided",
  "org.scaldi" %% "scaldi-jsr330" % "0.5.9",
  "org.scalatest" %% "scalatest" % "3.0.3" % "test",
  "com.typesafe.play" %% "play-test" % playVersion % "test",
  "com.typesafe.play" %% "play-slick" % slickVersion % "test",
  "com.typesafe.play" %% "play-slick-evolutions" % slickVersion % "test",
  "com.h2database" % "h2" % "1.4.187" % "test",
  "com.typesafe.play" %% "play-cache" % playVersion % "test" // cache plugin add extra bindings which have some specialties and will be tested automatically
)

git.remoteRepo := "git@github.com:scaldi/scaldi-play.git"
resolvers ++= Seq(
  "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases")

// Site and docs

site.settings
site.includeScaladoc()
ghpages.settings

// Publishing

publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := (_ => false)
publishTo := Some(
  if (version.value.trim.endsWith("SNAPSHOT"))
    "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")

// nice prompt!

shellPrompt in ThisBuild := { state =>
  scala.Console.GREEN + Project.extract(state).currentRef.project + "> " + scala.Console.RESET
}

// Additional meta-info

startYear := Some(2011)
organizationHomepage := Some(url("https://github.com/scaldi"))
scmInfo := Some(ScmInfo(
  browseUrl = url("https://github.com/scaldi/scaldi-play"),
  connection = "scm:git:git@github.com:scaldi/scaldi-play.git"
))
pomExtra := <xml:group>
  <developers>
    <developer>
      <id>OlegIlyenko</id>
      <name>Oleg Ilyenko</name>
    </developer>
  </developers>
</xml:group>
