name := "dipendi-play"
organization := "com.protenus"

description := "Dipendi-Play - Dipendi integration for Play framework"
homepage := Some(url("http://github.com/protenus/dipendi-play"))
licenses := Seq("Apache License, ASL Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

crossScalaVersions := Seq("2.11.12", "2.12.10", "2.13.1")
scalaVersion := "2.13.1"

scalacOptions ++= Seq("-deprecation", "-feature")
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
testOptions in Test += Tests.Argument("-oDF")

val playVersion = "2.7.4"
val slickVersion = "4.0.2"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % playVersion % Provided,
  "com.typesafe.play" %% "play-guice" % playVersion % Provided,
  "com.protenus" %% "dipendi" % "0.6.0",
  "com.protenus" %% "dipendi-jsr330" % "0.6.0",
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  "com.typesafe.play" %% "play-test" % playVersion % Test,
  "com.typesafe.play" %% "play-slick" % slickVersion % Test,
  "com.typesafe.play" %% "play-slick-evolutions" % slickVersion % Test,
  "com.h2database" % "h2" % "1.4.196" % Test,
  "com.typesafe.play" %% "play-cache" % playVersion % Test // cache plugin add extra bindings which have some specialties and will be tested automatically
)

git.remoteRepo := "git@github.com:protenus/dipendi-play.git"

// Site and docs

enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)

// Publishing

publishArtifact in Test := false
pomIncludeRepository := (_ => false)

// nice prompt!

shellPrompt in ThisBuild := { state =>
  scala.Console.GREEN + Project.extract(state).currentRef.project + "> " + scala.Console.RESET
}

// Additional meta-info

startYear := Some(2011)
organizationHomepage := Some(url("https://github.com/protenus"))
developers := Developer("AprilAtProtenus", "April Hyacinth", "", url("https://github.com/AprilAtProtenus")) :: Nil
scmInfo := Some(ScmInfo(
  browseUrl = url("https://github.com/protenus/dipendi-play"),
  connection = "scm:git:git@github.com:protenus/dipendi-play.git"
))
