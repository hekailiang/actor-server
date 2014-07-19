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
    "newzly repository"   at "http://maven.newzly.com/repository/internal",
    "secret repository"   at "http://repos.81port.com/content/repositories/secret",
    "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven",
    mavenOrg
  )

}
