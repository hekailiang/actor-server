import sbt._
import sbt.Keys._
import akka.sbt.AkkaKernelPlugin
import akka.sbt.AkkaKernelPlugin.{ Dist, outputDirectory, distJvmOptions, distBootClass }
import org.flywaydb.sbt.FlywayPlugin._
import play.PlayScala
import sbtprotobuf.{ ProtobufPlugin => PB }
import scalabuff.ScalaBuffPlugin._
import spray.revolver.RevolverPlugin._

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
        initialize                ~= { _ => sys.props("scalac.patmat.analysisBudget") = "off" },
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
   .aggregate(actorTests, actorPersist, actorProtobuf)

  lazy val akkaPersistenceSqlAsync = uri("git://github.com/prettynatty/akka-persistence-sql-async.git")

  lazy val actorUtil = Project(
    id   = "actor-util",
    base = file("actor-util"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.util
    )
  )

  lazy val actorTestkit = Project(
    id   = "actor-testkit",
    base = file("actor-testkit"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.testkit
    )
  )

  lazy val actorProtobuf = Project(
    id       = "actor-protobuf",
    base     = file("actor-protobuf"),
    settings = defaultSettings ++ scalabuffSettings ++ PB.protobufSettings ++ Seq(
      //sourceDirectory in PB.protobufConfig := file("src/main/protobuf-java"),
      sourceDirectory in PB.protobufConfig <<= (sourceDirectory in Compile) { _ / "protobuf-java" },
      libraryDependencies ++= Dependencies.messages
    )
  ).configs(ScalaBuff)

  lazy val actorModels = Project(
    id       = "actor-models",
    base     = file("actor-models"),
    settings = defaultSettings
  )
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .dependsOn(actorProtobuf)

  lazy val actorPersist = Project(
    id       = "actor-persist",
    base     = file("actor-persist"),
    settings = defaultSettings ++ flywaySettings ++ Seq(
      flywayUrl := "jdbc:postgresql://localhost:5432/actor",
      flywayUser := "postgres",
      flywayPassword := "",
      flywaySchemas := Seq("public"),
      flywayLocations := Seq("sql/migration")
    )
  ).dependsOn(actorModels, actorProtobuf, actorTestkit % "test")

  lazy val actorRestApi = Project(
    id       = "actor-restapi",
    base     = file("actor-restapi"),
    settings = Seq(
      libraryDependencies ++= Dependencies.restApi,
      javaOptions in Test += "-Dconfig.file=conf/test.conf",
      scalaVersion        := ScalaVersion
    )
  ).enablePlugins(PlayScala)
   .dependsOn(actorModels, actorPersist)

  lazy val actorApi = Project(
    id       = "actor-api",
    base     = file("actor-api"),
    settings = defaultSettings
  ).dependsOn(actorPersist, actorUtil, actorProtobuf, akkaPersistenceSqlAsync)

  lazy val actorTests = Project(
    id       = "actor-tests",
    base     = file("actor-tests"),
    settings = defaultSettings
  ).dependsOn(actorApi, actorModels, actorPersist, actorTestkit % "test")
}
