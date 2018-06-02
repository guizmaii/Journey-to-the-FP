name := "async_magic_tricks"

version := "1.0"

lazy val scala212 = "2.12.6"
lazy val scala211 = "2.11.12"

scalaVersion := scala212
crossScalaVersions := Seq(scala211, scala212)

scalafmtOnCompile in ThisBuild := true

lazy val monix = "io.monix" %% "monix" % "3.0.0-RC1"

lazy val testKit =
  Seq(
    "org.scalacheck" %% "scalacheck" % "1.14.0",
    "org.scalatest"  %% "scalatest"  % "3.0.5",
  ).map(_ % Test)

libraryDependencies += monix
libraryDependencies ++= testKit
