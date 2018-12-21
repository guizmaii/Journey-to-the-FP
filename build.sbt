name := "Journey-to-the-FP"

version := "1.0"

lazy val scala212 = "2.12.8"
lazy val scala211 = "2.11.12"

scalaVersion := scala212
crossScalaVersions := Seq(scala211, scala212)

scalafmtOnCompile in ThisBuild := true

addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.9" cross CrossVersion.binary)

libraryDependencies += "io.monix"          %% "monix"         % "3.0.0-RC2"
libraryDependencies += "org.typelevel"     %% "cats-core"     % "1.5.0"
libraryDependencies += "org.typelevel"     %% "cats-effect"   % "1.1.0"
libraryDependencies += "io.chrisdavenport" %% "cats-par"      % "0.2.0"
libraryDependencies += "io.monix"          %% "minitest"      % "2.2.2" % Test
libraryDependencies += "io.monix"          %% "minitest-laws" % "2.2.2" % Test
libraryDependencies += "org.scalaz"        %% "scalaz-zio"    % "0.5.1" % Test

testFrameworks += new TestFramework("minitest.runner.Framework")

scalacOptions := scalacOptions.value.filter(_ != "-Xfatal-warnings")
