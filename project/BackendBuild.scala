import sbt._
import sbt.Keys._
import akka.sbt.AkkaKernelPlugin
import akka.sbt.AkkaKernelPlugin.{Dist, outputDirectory, distJvmOptions, distBootClass}
import spray.revolver.RevolverPlugin._
import org.scalastyle.sbt.ScalastylePlugin

//import com.typesafe.sbt.SbtAtmos.{atmosSettings, Atmos}

object BackendBuild extends Build {
  val Organization = "secretapp"
  val Version = "0.1-SNAPSHOT"
  val ScalaVersion = "2.10.4"

  import Dependencies._
  import TestDependencies._
  import CompileDependencies._

  val appName = "backend"
  val appClass = "com.secretapp.backend.ApiKernel"
  val appClassMock = "com.secretapp.backend.Main"

  lazy val rootDependencies = akka ++ scalaz ++ cassandra ++ etc ++ testDependencies

  lazy val root = Project(
    appName,
    file("."),
    settings = defaultSettings ++
      AkkaKernelPlugin.distSettings ++
      Revolver.settings ++
      //      AtmosDist.settings ++
      ScalastylePlugin.Settings ++
      Seq(
        libraryDependencies ++= rootDependencies,
        resolvers ++= Resolvers.seq,
        distJvmOptions in Dist := "-Xms256M -Xmx1024M",
        distBootClass in Dist := appClass,
        outputDirectory in Dist := file("target/dist"),
        Revolver.reStartArgs := Seq(appClassMock),
        mainClass in Revolver.reStart := Some(appClassMock),
        fork in Test := true
      )
  ).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
  // .settings(atmosSettings: _*).configs(Atmos)

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := Organization,
    version := Version,
    scalaVersion := ScalaVersion,
    crossPaths := false,
    organizationName := "Secret LLC.",
    organizationHomepage := Some(url("https://secretapp.io"))
  )

  lazy val defaultSettings = buildSettings ++ Seq(
    resolvers ++= Resolvers.seq,
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature"), //, "-Xprint:typer"
    javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")
  )
}
