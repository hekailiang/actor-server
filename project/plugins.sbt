resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/",
  Resolver.url("secret repository", url("http://54.77.139.175:8081/content/repositories/snapshots"))(Resolver.ivyStylePatterns)
)

addSbtPlugin("com.typesafe.akka" % "akka-sbt-plugin" % "2.2.3")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

addSbtPlugin("com.github.sbt" % "sbt-scalabuff" % "1.3.8-SNAPSHOT-SECRET")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.6")
