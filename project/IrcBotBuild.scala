import sbt._
import Keys._

object IrcBotBuild extends Build {

  import Dependencies._

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "fi.pyppe.ircbot",
    version      := "0.1-SNAPSHOT",
    scalaVersion := "2.10.2",
    offline := true
  )

  lazy val root = Project(
    id = "akka-ircbot",
    base = file("."),
    settings = buildSettings,
    aggregate = Seq(common, master, slave)
  )

  lazy val common = Project(
    id = "common",
    base = file("common"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= commonLibs
    )
  )

  lazy val master = Project(
    id = "master",
    base = file("master"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= commonLibs ++ masterLibs
    )
  ).dependsOn(common)

  lazy val slave = Project(
    id = "slave",
    base = file("slave"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= commonLibs
    )
  ).dependsOn(common)

}

object Dependencies {

  private val scalaLangExclusions = ExclusionRule(organization = "org.scala-lang")

  val commonLibs = Seq(
    "com.typesafe.akka"  %% "akka-remote"          % "2.2.1",
    "com.typesafe.akka"  %% "akka-slf4j"           % "2.2.1",
    "com.typesafe"       %  "config"               % "1.0.2",
    "com.typesafe"       %% "scalalogging-slf4j"   % "1.0.1" excludeAll(scalaLangExclusions),
    "org.joda"           %  "joda-convert"         % "1.5",
    "joda-time"          %  "joda-time"            % "2.3",
    "ch.qos.logback"     %  "logback-classic"      % "1.0.13"
  )

  val masterLibs = Seq(
    "org.pircbotx"       %  "pircbotx"             % "2.1-SNAPSHOT"
  )


}