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

  val typesafe = "typesafe repo" at "http://repo.typesafe.com/typesafe/releases"

  val sonatypeSnapshots = "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

  val sonatypeReleases = "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"

//  val localMaven = "Local Maven Repository" at "file:///" + Path.userHome.absolutePath + "/.m2/repository"

  lazy val seq = Seq(typesafe, mavenOrg, sonatypeReleases, sonatypeSnapshots) //, localMaven)

}
