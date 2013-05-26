name := "scaldi-play"

organization := "com.github.scaldi"

version := "0.1.1"

crossScalaVersions := Seq("2.10.1")

scalaVersion := "2.10.1"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
    "play" %% "play" % "2.1.1",
    "com.github.scaldi" %% "scaldi" % "0.1.2",
    "org.scalatest" %% "scalatest" % "1.9.1" % "test"
)

resolvers += "Angelsmasterpiece repo" at "https://raw.github.com/OlegIlyenko/angelsmasterpiece-maven-repo/master"