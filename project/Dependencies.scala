import sbt._

object Dependencies {
  object V {
    val akka = "2.3.6"
    val scalaz = "7.1.0" // "7.1.0-RC1"
    val scalazContrib = "0.1.5"
    val spray = "1.3.1"
    val phantom = "1.2.8"
    val newzlyUtil = "0.1.19"
  }

  val compileDependencies = Seq(
    "com.typesafe.akka" %% "akka-actor" % V.akka,
    "com.typesafe.akka" %% "akka-agent" % V.akka,
    "com.typesafe.akka" %% "akka-remote" % V.akka,
    "com.typesafe.akka" %% "akka-kernel" % V.akka,
    "com.typesafe.akka" %% "akka-cluster" % V.akka,
    "com.typesafe.akka" %% "akka-persistence-experimental" % V.akka,
    "com.github.krasserm" %% "akka-persistence-cassandra" % "0.3.3",
    "com.typesafe.akka" %% "akka-stream-experimental" % "0.4",
    "com.typesafe.akka" %% "akka-http-core-experimental" % "0.4",
    "com.typesafe.akka" %% "akka-contrib" % V.akka,
//    val akkaStreams = "com.typesafe.akka" % "akka-stream-experimental_2.11" % "0.4"
    "com.typesafe.akka" %% "akka-slf4j" % V.akka,

    "net.databinder.dispatch" %% "dispatch-core" % "0.11.1",

    "com.gilt" %% "gfc-timeuuid" % "0.0.5",

    "org.scalaz" %% "scalaz-core" % V.scalaz,
    "org.scalaz" %% "scalaz-concurrent" % V.scalaz,
    "org.scalaz" %% "scalaz-effect" % V.scalaz,
    "org.scalaz" %% "scalaz-typelevel" % V.scalaz,
    "org.scalaz" %% "scalaz-iteratee" % V.scalaz,
//    "org.typelevel" %% "scalaz-contrib-210"        % V.scalazContrib,
//    "org.typelevel" %% "scalaz-contrib-validation" % V.scalazContrib,
//    "org.typelevel" %% "scalaz-contrib-undo"       % V.scalazContrib,
//    // currently unavailable because there's no 2.11 build of Lift yet
//    // "org.typelevel" %% "scalaz-lift"               % "0.2",
//    "org.typelevel" %% "scalaz-nscala-time"        % V.scalazContrib,
//    "org.typelevel" %% "scalaz-spire"              % V.scalazContrib,

    "com.sksamuel.scrimage" %% "scrimage-core" % "1.4.1",

    "ch.qos.logback" % "logback-classic" % "1.1.2",

    "org.typelevel" %% "scodec-core" % "1.2.0",

    "org.typelevel" %% "scodec-bits" % "1.0.2",

//    val scodecStream = "org.typelevel" %% "scodec-stream" % "1.0.0-SNAPSHOT"

//    "com.google.guava" % "guava" % "17.0",

//    "javax.annotation" % "javax.annotation-api" % "1.2",

    "org.scalautils" %% "scalautils" % "2.1.3",

    "com.websudos"  %% "phantom-dsl" % V.phantom,
    "com.websudos"  %% "phantom-udt" % V.phantom,

    "com.newzly" %% "util-testing" % V.newzlyUtil % "provided"  excludeAll(ExclusionRule(organization = "org.slf4j")),
//
//    val async = "org.scala-lang.modules" %% "scala-async" % "0.9.1"
//
//    val macrodebug = "com.softwaremill.scalamacrodebug" %% "macros" % "0.4"
//
//    val macwiew = "com.softwaremill.macwire" %% "macros" % "0.6"
//
  "com.chuusai" % "shapeless_2.10.4" % "2.1.0-SNAPSHOT" changing(),
  "org.typelevel" %% "shapeless-scalacheck" % "0.2",
  "org.typelevel" %% "shapeless-spire" % "0.2",
  "org.typelevel" %% "shapeless-scalaz" % "0.2",

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
    "org.bouncycastle" % "bcprov-jdk15on" % "1.50",
//    "org.bouncycastle" % "bcprov-ext-jdk15on" % "1.50",
    "com.github.nscala-time" %% "nscala-time" % "1.2.0",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    "io.spray" %% "spray-caching" % V.spray,
    "net.sandrogrzicic" %% "scalabuff-runtime" % "1.3.8",
    "net.sandrogrzicic" %% "scalabuff-compiler" % "1.3.8",
//    "org.scala-miniboxing.plugins" %% "miniboxing-runtime" % "0.3-SNAPSHOT" TODO: http://scala-miniboxing.org/using_sbt.html
    "com.logentries" % "logentries-appender" % "1.1.25"
  )

  val deployDependencies = Seq(
    "com.typesafe.atmos" % "trace-akka-2.2.1_2.11.0-M3" % "1.3.1"
  )

  val testDependencies = Seq(
    "org.specs2" %% "specs2" % "2.4.2" % "test",
    "com.typesafe.akka" %% "akka-testkit" % V.akka % "test",
    "org.scalamock" %% "scalamock-specs2-support" % "3.0.1" % "test",
    "org.scalacheck" %% "scalacheck" % "1.11.5" % "test",
    "org.typelevel" %% "scalaz-specs2" % "0.2" % "test"
  )

  lazy val rootDependencies = compileDependencies ++ testDependencies
}
