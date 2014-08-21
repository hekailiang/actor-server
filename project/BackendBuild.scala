import sbt._
import sbt.Keys._
import akka.sbt.AkkaKernelPlugin
import akka.sbt.AkkaKernelPlugin.{Dist, outputDirectory, distJvmOptions, distBootClass}
import spray.revolver.RevolverPlugin._
import org.scalastyle.sbt.ScalastylePlugin
import scalabuff.ScalaBuffPlugin._

//import com.typesafe.sbt.SbtAtmos.{atmosSettings, Atmos}

object BackendBuild extends Build {
  val Organization = "secretapp"
  val Version = "0.1-SNAPSHOT"
  val ScalaVersion = "2.10.4"

  import Dependencies._

  val appName = "backend"
  val appClass = "com.secretapp.backend.ApiKernel"
  val appClassMock = "com.secretapp.backend.Main"

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
        distJvmOptions in Dist := "-server -Xms256M -Xmx1024M",
        distBootClass in Dist := appClass,
        outputDirectory in Dist := file("target/dist"),
        Revolver.reStartArgs := Seq(appClassMock),
        mainClass in Revolver.reStart := Some(appClassMock),
        autoCompilerPlugins := true,
        scalacOptions in (Compile,doc) := Seq("-groups", "-implicits", "-diagrams")
        //wartremoverExcluded ++= Seq("com.secretapp.backend.ApiKernel"),
        //wartremoverErrors in (Compile, compile) ++= Warts.all
      )
  ).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*).configs(ScalaBuff)
  // .settings(atmosSettings: _*).configs(Atmos)

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := Organization,
    version := Version,
    scalaVersion := ScalaVersion,
    crossPaths := false,
    organizationName := "Secret LLC.",
    organizationHomepage := Some(url("https://secretapp.io"))
  )

  lazy val defaultSettings = buildSettings ++ scalabuffSettings ++ Seq(
    resolvers ++= Resolvers.seq,
    scalacOptions ++= Seq("-target:jvm-1.7", "-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature"), //, "-Xprint:typer"
    javacOptions ++= Seq(
      "-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation"
    ),
    parallelExecution in Test := true,
    fork in Test := true
  )
}
