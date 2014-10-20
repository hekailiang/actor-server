import sbt._

object Dependencies {
  object V {
    val akka          = "2.3.6"
    val newzlyUtil    = "0.1.19"
    val phantom       = "1.2.8"
    val scalaz        = "7.1.0"
    val scalazContrib = "0.1.5"
    val spray         = "1.3.2"
  }

  val compileDependencies = Seq(
    "ch.qos.logback"              % "logback-classic"               % "1.1.2",
    "com.chuusai"                 % "shapeless_2.10.4"              % "2.1.0-SNAPSHOT" changing(),
    "com.gilt"                   %% "gfc-timeuuid"                  % "0.0.5",
    "com.github.krasserm"        %% "akka-persistence-cassandra"    % "0.3.4",
    "com.github.nscala-time"     %% "nscala-time"                   % "1.2.0",
    "com.logentries"              % "logentries-appender"           % "1.1.25",
    "com.newzly"                 %% "util-testing"                  % V.newzlyUtil % "provided" excludeAll(ExclusionRule(organization = "org.slf4j")),
    "com.notnoop.apns"           %  "apns"                          % "1.0.0.Beta4",
    "com.sksamuel.scrimage"      %% "scrimage-core"                 % "1.4.1",
    "com.typesafe.akka"          %% "akka-actor"                    % V.akka,
    "com.typesafe.akka"          %% "akka-agent"                    % V.akka,
    "com.typesafe.akka"          %% "akka-cluster"                  % V.akka,
    "com.typesafe.akka"          %% "akka-contrib"                  % V.akka,
    "com.typesafe.akka"          %% "akka-http-core-experimental"   % "0.4",
    "com.typesafe.akka"          %% "akka-kernel"                   % V.akka,
    "com.typesafe.akka"          %% "akka-persistence-experimental" % V.akka,
    "com.typesafe.akka"          %% "akka-remote"                   % V.akka,
    "com.typesafe.akka"          %% "akka-slf4j"                    % V.akka,
    "com.typesafe.akka"          %% "akka-stream-experimental"      % "0.4",
    "com.typesafe.play"          %% "play-json"                     % "2.4.0-M1",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j"           % "2.1.2",
    "com.websudos"               %% "phantom-dsl"                   % V.phantom,
    "com.websudos"               %% "phantom-udt"                   % V.phantom,
    "com.wandoulabs.akka"        %% "spray-websocket"               % "0.1.4-SNAPSHOT" excludeAll(ExclusionRule(organization = "com.chuusai")),
    "io.spray"                   %% "spray-caching"                 % V.spray,
    "net.databinder.dispatch"    %% "dispatch-core"                 % "0.11.1",
    "net.sandrogrzicic"          %% "scalabuff-compiler"            % "1.3.8",
    "net.sandrogrzicic"          %% "scalabuff-runtime"             % "1.3.8",
    "org.bouncycastle"            % "bcprov-jdk15on"                % "1.50",
    "org.scalautils"             %% "scalautils"                    % "2.1.3",
    "org.scalaz"                 %% "scalaz-concurrent"             % V.scalaz,
    "org.scalaz"                 %% "scalaz-core"                   % V.scalaz,
    "org.scalaz"                 %% "scalaz-effect"                 % V.scalaz,
    "org.scalaz"                 %% "scalaz-iteratee"               % V.scalaz,
    "org.scalaz"                 %% "scalaz-typelevel"              % V.scalaz,
    "org.typelevel"              %% "scodec-bits"                   % "1.0.2",
    "org.typelevel"              %% "scodec-core"                   % "1.2.0",
    "org.typelevel"              %% "shapeless-scalacheck"          % "0.2",
    "org.typelevel"              %% "shapeless-scalaz"              % "0.2",
    "org.typelevel"              %% "shapeless-spire"               % "0.2"
  )

  val deployDependencies = Seq(
    "com.typesafe.atmos"          % "trace-akka-2.2.1_2.11.0-M3"    % "1.3.1"
  )

  val testDependencies = Seq(
    "com.typesafe.akka"          %% "akka-testkit"                  % V.akka       % "test",
    "org.scalacheck"             %% "scalacheck"                    % "1.11.5"     % "test",
    "org.scalamock"              %% "scalamock-specs2-support"      % "3.0.1"      % "test",
    "org.specs2"                 %% "specs2"                        % "2.4.6"      % "test",
    "org.typelevel"              %% "scalaz-specs2"                 % "0.2"        % "test"
  )

  lazy val rootDependencies = compileDependencies ++ testDependencies
}
