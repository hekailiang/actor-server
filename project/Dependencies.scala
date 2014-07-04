import sbt._

object Dependencies {

  object V {
    val akka = "2.3.3"
    val scalaz = "7.0.6"
    val phantom = "0.8.0"
  }

  object CompileDependencies {

    val akkaActor = "com.typesafe.akka" %% "akka-actor" % V.akka

    val akkaAgent = "com.typesafe.akka" %% "akka-agent" % V.akka

    val akkaRemote = "com.typesafe.akka" %% "akka-remote" % V.akka

    val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % V.akka

    val akkaKernel = "com.typesafe.akka" %% "akka-kernel" % V.akka

//    val akkaStreams = "com.typesafe.akka" % "akka-stream-experimental_2.11" % "0.4"

    val dispatch = "net.databinder.dispatch" %% "dispatch-core" % "0.11.1"

    val logging = "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.0.4"

    val scalazCore = "org.scalaz" %% "scalaz-core" % V.scalaz

    val scalazConcurrent = "org.scalaz" %% "scalaz-concurrent" % V.scalaz

    val redis = "com.etaty.rediscala" %% "rediscala" % "1.3.1"

    val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"

    val scodec = "org.typelevel" %% "scodec-core" % "1.1.0"

    val scodecBits = "org.typelevel" %% "scodec-bits" % "1.0.1"

//    val scodecStream = "org.typelevel" %% "scodec-stream" % "1.0.0-SNAPSHOT"

    val guava = "com.google.guava" % "guava" % "17.0"

    val annotationApi = "javax.annotation" % "javax.annotation-api" % "1.2"

    val scalaUtils = "org.scalautils" %% "scalautils" % "2.1.3"

    val phantomDsl = "com.newzly"  %% "phantom-dsl" % V.phantom
    val phantomCassandraUnit = "com.newzly"  %% "phantom-cassandra-unit" % V.phantom
    // val phantomExample = "com.newzly"  %% "phantom-example" % V.phantom
    val phantomThrift = "com.newzly"  %% "phantom-thrift" % V.phantom
    val phantomTest = "com.newzly"  %% "phantom-test" % V.phantom
//
//    val async = "org.scala-lang.modules" %% "scala-async" % "0.9.1"
//
//    val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "1.0.0"
//
//    val macrodebug = "com.softwaremill.scalamacrodebug" %% "macros" % "0.4"
//
//    val macwiew = "com.softwaremill.macwire" %% "macros" % "0.6"
//
    val shapeless = "com.chuusai" % "shapeless_2.10.4" % "2.1.0-SNAPSHOT"
//
//    val optional = "org.nalloc" %% "optional" % "0.1.0"
//
//    val platformEx = "com.nocandysw" %% "platform-executing" % "0.5.0"
//
//    val stateless = "com.qifun" %% "stateless-future" % "0.1.1"
//
//    val faststring = "com.dongxiguo" %% "fastring" % "0.2.4"
//
//    val applyBuilder = "com.github.xuwei-k" %% "applybuilder70" % "0.1.2"
//
//    val nobox = "com.github.xuwei-k" %% "nobox" % "0.1.9"
//
//    val stm = "org.scala-stm" %% "scala-stm" % "0.7"
//
//    val parboiled = "org.parboiled" %% "parboiled-scala" % "1.1.6"
//
//    val scalaEquals = "org.scalaequals" %% "scalaequals-core" % "1.2.0"
//
//    val monocle = "com.github.julien-truffaut" %% "monocle-core" % "0.3.0"
//
//    val scalacache = "com.github.cb372" %% "scalacache-guava" % "0.3.0"

  }

  object DeployDependencies {

    val atmos = "com.typesafe.atmos" % "trace-akka-2.2.1_2.11.0-M3" % "1.3.1"

  }

  object TestDependencies {
    val scalatest = "org.scalatest" %% "scalatest" % "2.2.0" % "test"

    val akkaTest = "com.typesafe.akka" %% "akka-testkit" % V.akka % "test"

//    val scalaMockTest = "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test"

    val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.11.4" % "test"
  }


  import CompileDependencies._
  import DeployDependencies._
  import TestDependencies._

  val akka = Seq(akkaKernel, akkaActor, akkaAgent, akkaRemote, akkaSlf4j, /* akkaStreams, */ logback)

  val scalaz = Seq(scalazCore, scalazConcurrent)

  val dbs = Seq(redis)

  val cassandra = Seq(phantomDsl, phantomCassandraUnit, phantomThrift, phantomTest)

  val etc = Seq(guava, annotationApi, scodec, scodecBits, shapeless, scalaUtils, dispatch)

  val testDependencies = Seq(scalatest, akkaTest, scalaCheck)

}
