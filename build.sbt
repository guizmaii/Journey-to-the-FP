name := "async_magic_tricks"

version := "1.0"

lazy val scala212 = "2.12.6"
lazy val scala211 = "2.11.12"

scalaVersion := scala212
crossScalaVersions := Seq(scala211, scala212)

scalafmtOnCompile in ThisBuild := true

val monix = "io.monix" %% "monix" % "3.0.0-RC1"

libraryDependencies += monix

