name         := "scaldi-play"
organization := "org.scaldi"

description := "Scaldi-Play - Scaldi integration for Play framework"
homepage    := Some(url("http://github.com/scaldi/scaldi-play"))
licenses    := Seq("Apache License, ASL Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

scalaVersion          := "2.13.8"
crossScalaVersions    := Seq("2.12.15", "2.13.8")
mimaPreviousArtifacts := Set("0.6.0", "0.6.1").map(organization.value %% name.value % _)
scalacOptions ++= Seq("-deprecation", "-feature")
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
Test / testOptions += Tests.Argument("-oDF")

val playVersion  = "2.8.13"
val slickVersion = "5.0.2"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play"                  % playVersion  % Provided,
  "com.typesafe.play" %% "play-guice"            % playVersion  % Provided,
  "org.scaldi"        %% "scaldi-jsr330"         % "0.6.2",
  "org.scalatest"     %% "scalatest"             % "3.2.16"     % Test,
  "com.typesafe.play" %% "play-test"             % playVersion  % Test,
  "com.typesafe.play" %% "play-slick"            % slickVersion % Test,
  "com.typesafe.play" %% "play-slick-evolutions" % slickVersion % Test,
  "com.h2database"     % "h2"                    % "2.1.212"    % Test,
  "com.typesafe.play" %% "play-cache" % playVersion % Test // cache plugin add extra bindings which have some specialties and will be tested automatically
)

git.remoteRepo := "git@github.com:scaldi/scaldi-play.git"

// Publishing

pomIncludeRepository   := (_ => false)
Test / publishArtifact := false
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowScalaVersions         := crossScalaVersions.value
ThisBuild / githubWorkflowJavaVersions          := Seq("8", "11").map(JavaSpec(JavaSpec.Distribution.Adopt, _))
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowBuildPreamble         := Seq(WorkflowStep.Sbt(List("scalafmtCheckAll")))
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE"    -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET"        -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

// Site and docs

enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)

// nice prompt!

ThisBuild / shellPrompt := { state =>
  scala.Console.GREEN + Project.extract(state).currentRef.project + "> " + scala.Console.RESET
}

// Additional meta-info

startYear            := Some(2011)
organizationHomepage := Some(url("https://github.com/scaldi"))
scmInfo := Some(
  ScmInfo(
    browseUrl = url("https://github.com/scaldi/scaldi-play"),
    connection = "scm:git:git@github.com:scaldi/scaldi-play.git"
  )
)
developers := List(
  Developer("AprilAtProtenus", "April Hyacinth", "april@protenus.com", url("https://github.com/AprilAtProtenus")),
  Developer("dave-handy", "Dave Handy", "wdhandy@gmail.com", url("https://github.com/dave-handy"))
)

scalafmtCheckAll := {
  (Compile / scalafmtSbtCheck).value
  scalafmtCheckAll.value
}

scalafmtAll := {
  (Compile / scalafmtSbt).value
  scalafmtAll.value
}
