import sbt._

object Dependencies {
  object V {
    val akka    = "2.3.7"
    val phantom = "1.4.0"
    val scalaz  = "7.1.0"
  }

  object Compile {
    val apns            = "com.notnoop.apns"              %  "apns"                          % "1.0.0.Beta4"
    val scrImageCore    = "com.sksamuel.scrimage"         %% "scrimage-core"                 % "1.4.1"
    val akkaActor       = "com.typesafe.akka"             %% "akka-actor"                    % V.akka
    val akkaCluster     = "com.typesafe.akka"             %% "akka-cluster"                  % V.akka
    val akkaContrib     = "com.typesafe.akka"             %% "akka-contrib"                  % V.akka
    val akkaKernel      = "com.typesafe.akka"             %% "akka-kernel"                   % V.akka
    val akkaPersistence = "com.typesafe.akka"             %% "akka-persistence-experimental" % V.akka
    val akkaPersistenceCassandra = "com.github.krasserm"           %% "akka-persistence-cassandra"    % "0.3.4"
    val akkaRemote      = "com.typesafe.akka"             %% "akka-remote"                   % V.akka
    val akkaSlf4j       = "com.typesafe.akka"             %% "akka-slf4j"                    % V.akka
    val akkaStream      = "com.typesafe.akka"             %% "akka-stream-experimental"      % "0.4"
    val sprayWebSocket  = "com.wandoulabs.akka"           %% "spray-websocket"               % "0.1.4-SNAPSHOT" excludeAll(ExclusionRule(organization = "com.chuusai"))
    val sprayClient     = "io.spray"                      %% "spray-client"                  % "1.3.2"
    // we need this because commons-codec 1.2 jar is broken (apns dependency)
    val commonsCodec    = "commons-codec"                 %  "commons-codec"                 % "1.3"
    val dispatchCore    = "net.databinder.dispatch"       %% "dispatch-core"                 % "0.11.1"
    val bcprov          = "org.bouncycastle"              %  "bcprov-jdk15on"                % "1.50"
    val scodecBits      = "org.typelevel"                 %% "scodec-bits"                   % "1.0.2"
    val scodecCore      = "org.typelevel"                 %% "scodec-core"                   % "1.2.0"
    val libPhoneNumber  = "com.googlecode.libphonenumber" % "libphonenumber"                 % "7.0"
    val clinkedhashmap  = "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.2_jdk5"
    val phantomDsl      = "com.websudos"                  %% "phantom-dsl"                   % V.phantom
    val scalazCore      = "org.scalaz"                    %% "scalaz-core"                   % V.scalaz
    val logbackClassic  = "ch.qos.logback"                % "logback-classic"                % "1.1.2"
    val logbackLogstash = "net.logstash.logback"          % "logstash-logback-encoder"       % "3.3"

    val scalalikeAsync  = "org.scalikejdbc"               %% "scalikejdbc-async"             % "0.5.+"
    val postgresAsync   = "com.github.mauricio"           %% "postgresql-async"              % "0.2.+"

    val postgresJdbc    = "postgresql"                    %  "postgresql"                    % "9.1-901-1.jdbc4"

    val flywayCore      = "org.flywaydb"                  %  "flyway-core"                   % "3.1"
  }

  object Test {
    val akkaTestkit       = "com.typesafe.akka"          %% "akka-testkit"                  % V.akka               % "test"
    val scalacheck        = "org.scalacheck"             %% "scalacheck"                    % "1.11.6"             % "test"
    val specs2            = "org.specs2"                 %% "specs2-core"                   % "2.4.11"
    val scalazSpecs2      = "org.typelevel"              %% "scalaz-specs2"                 % "0.3.0"              % "test"
    val utilTesting       = "com.websudos"               %% "util-testing"                  % "0.3.12"             % "test" excludeAll(ExclusionRule(organization = "org.slf4j"))
    val scalaLoggingSlf4j = "com.typesafe.scala-logging" %% "scala-logging-slf4j"           % "2.1.2"              % "test"
  }

  object Deploy {
    val traceAkka = "com.typesafe.atmos" % "trace-akka-2.2.1_2.11.0-M3" % "1.3.1"
  }

  import Compile._, Test._, Deploy._

  val common    = Seq(logbackClassic, logbackLogstash)

  val util      = Seq(akkaActor, akkaSlf4j)

  val api       = common ++ Seq(
    apns, scrImageCore, akkaActor, akkaContrib, akkaKernel, akkaPersistence, sprayWebSocket, commonsCodec, akkaCluster,
    clinkedhashmap, dispatchCore, bcprov, scodecBits, scodecCore, libPhoneNumber, akkaSlf4j, akkaPersistenceCassandra,
    sprayClient
  )

  val deploy    = Seq(traceAkka)

  val models    = common ++ Seq(scodecBits, akkaPersistence)

  val messages  = Seq(akkaActor)

  val persist   = common ++ Seq(
    akkaActor, flywayCore, scodecBits, phantomDsl, scalazCore, scalalikeAsync, postgresAsync, postgresJdbc,
    specs2
  )

  val root      = common ++ Seq(akkaCluster, akkaSlf4j, scalaLoggingSlf4j, akkaKernel)

  val tests     = common ++ Seq(
    akkaCluster, akkaTestkit, akkaSlf4j, scalacheck, specs2, scalazSpecs2, utilTesting,
      scalaLoggingSlf4j, akkaKernel
  )

  val restApi   = Seq(phantomDsl, scodecBits, bcprov, specs2, scalazCore)

}
