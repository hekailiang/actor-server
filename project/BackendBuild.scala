import sbt._
import sbt.Keys._
import akka.sbt.AkkaKernelPlugin
import akka.sbt.AkkaKernelPlugin.{Dist, outputDirectory, distJvmOptions, distBootClass}
import spray.revolver.RevolverPlugin._
import scalabuff.ScalaBuffPlugin._

object BackendBuild extends Build {
  val Organization = "Actor IM"
  val Version = "0.1-SNAPSHOT"
  val ScalaVersion = "2.10.4"

  val appName = "backend"
  val appClass = "com.secretapp.backend.ApiKernel"
  val appClassMock = "com.secretapp.backend.Main"


  lazy val buildSettings =
    Defaults.defaultSettings ++
      Seq(
        organization         := Organization,
        version              := Version,
        scalaVersion         := ScalaVersion,
        crossPaths           := false,
        organizationName     := Organization,
        organizationHomepage := Some(url("https://actor.im"))
      )

  lazy val defaultSettings =
    buildSettings ++
      Seq(
        resolvers                 ++= Resolvers.seq,
        scalacOptions             ++= Seq("-target:jvm-1.7", "-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature", "-language:higherKinds"),
        javaOptions               ++= Seq("-Dfile.encoding=UTF-8", "-XX:MaxPermSize=1024m"),
        javacOptions              ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation"),
        parallelExecution in Test :=  false,
        fork              in Test :=  true
      )

  lazy val root = Project(
    appName,
    file("."),
    settings =
      defaultSettings               ++
      AkkaKernelPlugin.distSettings ++
      Revolver.settings             ++
      Seq(
        libraryDependencies                       ++= Dependencies.root,
        distJvmOptions       in Dist              :=  "-server -Xms256M -Xmx1024M",
        distBootClass        in Dist              :=  appClass,
        outputDirectory      in Dist              :=  file("target/dist"),
        Revolver.reStartArgs                      :=  Seq(appClassMock),
        mainClass            in Revolver.reStart  :=  Some(appClassMock),
        autoCompilerPlugins                       :=  true,
        scalacOptions        in (Compile,doc)     :=  Seq("-groups", "-implicits", "-diagrams")
      )
  ).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
   .dependsOn(actorApi, actorModels, actorPersist)
   .aggregate(actorTests)

  lazy val actorExport2Js = Project(
    id       = "actor-export2js",
    base     = file("actor-export2js"),
    settings = Revolver.settings
  ).dependsOn(actorExport2JsMacros)

  lazy val actorExport2JsMacros = Project(
    id       = "actor-export2js-macros",
    base     = file("actor-export2js-macros"),
    settings = Revolver.settings
  ).dependsOn(root)

  lazy val actorUtil = Project(
    id   = "actor-util",
    base = file("actor-util"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.util
    )
  )

  lazy val actorModels = Project(
    id   = "actor-models",
    base = file("actor-models"),
    settings = defaultSettings
  )

  lazy val actorPersist = Project(
    id   = "actor-persist",
    base = file("actor-persist"),
    settings = defaultSettings
  ).dependsOn(actorModels)

  lazy val actorApi = Project(
    id       = "actor-api",
    base     = file("actor-api"),
    settings = defaultSettings ++ scalabuffSettings
  ).dependsOn(actorPersist, actorUtil).configs(ScalaBuff)

  lazy val actorTests = Project(
    id       = "actor-tests",
    base     = file("actor-tests"),
    settings = defaultSettings
  ).dependsOn(actorApi, actorModels, actorPersist)
}
