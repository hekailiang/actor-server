import sbt._

object Resolvers {
  val mavenOrg = {
    val r = new org.apache.ivy.plugins.resolver.IBiblioResolver
    r.setM2compatible(true)
    r.setName("maven repo")
    r.setRoot("http://repo1.maven.org/maven2/")
    r.setCheckconsistency(false)
    new RawRepository(r)
  }

  lazy val seq = Seq(
    "typesafe repo"       at "http://repo.typesafe.com/typesafe/releases",
    "sonatype snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots/",
    "sonatype releases"   at "https://oss.sonatype.org/content/repositories/releases/",
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
    "spray repo" at "http://repo.spray.io/",
    "Websudos releases" at "http://maven.websudos.co.uk/ext-release-local",
    Resolver.url("secret repository", url("http://54.77.139.175:8082/content/repositories/snapshots"))(Resolver.ivyStylePatterns),
    "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven",
    mavenOrg
  )

}
