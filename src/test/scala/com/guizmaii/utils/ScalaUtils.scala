package com.guizmaii.utils

object ScalaUtils {

  lazy val scalaVersion: String = util.Properties.versionString

  lazy val isScala211: Boolean = scalaVersion.contains("2.11")
  lazy val isScala212: Boolean = scalaVersion.contains("2.12")

  lazy val globalExecutionThreadPoolName: String = (isScala211, isScala212) match {
    case (true, _) => "ForkJoinPool-1-worker"
    case (_, true) => "scala-execution-context-global"
    case _         => throw new RuntimeException(s"This code is not yet compatible with Scala $scalaVersion")
  }

}
