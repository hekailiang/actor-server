import sbt._
import sbt.Keys._
import akka.sbt.AkkaKernelPlugin
import akka.sbt.AkkaKernelPlugin.{ Dist, outputDirectory, distJvmOptions, distBootClass }
import org.flywaydb.sbt.FlywayPlugin._
import sbtprotobuf.{ ProtobufPlugin => PB }
import scalabuff.ScalaBuffPlugin._
import spray.revolver.RevolverPlugin._

object BackendBuild extends Build {
  val Organization = "Actor IM"
  val Version = "0.1-SNAPSHOT"
  val ScalaVersion = "2.10.4"

  val appName = "backend"
  val appClass = "im.actor.server.ApiKernel"
  val appClassMock = "im.actor.server.Main"

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
        initialize                ~= { _ =>
          sys.props("scalac.patmat.analysisBudget") = "off"
          if (sys.props("java.specification.version") != "1.8")
            sys.error("Java 8 is required for this project.")
        },
        resolvers                 ++= Resolvers.seq,
        scalacOptions             ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-feature", "-language:higherKinds"),
        javaOptions               ++= Seq("-Dfile.encoding=UTF-8"),
        javacOptions              ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation"),
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
   .dependsOn(actorApi, actorSmtpd, actorRestApi)
   .aggregate(actorTests, actorPersist, actorProtobuf)

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
  ).dependsOn(actorModels, actorProtobuf, actorUtil, actorTestkit % "test")

  lazy val actorRestApi = Project(
    id       = "actor-restapi",
    base     = file("actor-restapi"),
    settings = Seq(
      libraryDependencies ++= Dependencies.restApi,
      javaOptions in Test += "-Dconfig.file=conf/test.conf",
      scalaVersion        := ScalaVersion
    )
  ).dependsOn(actorModels, actorPersist)

  lazy val actorApi = Project(
    id       = "actor-api",
    base     = file("actor-api"),
    settings = defaultSettings
  ).dependsOn(actorPersist, actorUtil, actorProtobuf)

  lazy val actorSmtpd = Project(
    id       = "actor-smtpd",
    base     = file("actor-smtpd"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.smtpd
    )
  ).dependsOn(actorApi)

  lazy val actorTests = Project(
    id       = "actor-tests",
    base     = file("actor-tests"),
    settings = defaultSettings
  ).dependsOn(actorApi, actorModels, actorPersist, actorTestkit % "test")

  lazy val actorSchema = Project(
    id       = "actor-schema",
    base     = file("actor-schema"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.schema
    )
  )
}
