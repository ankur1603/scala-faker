import sbt._
import Keys._
import sbtrelease.ReleasePlugin._
import ls.Plugin.LsKeys

object Dependencies {

  val snakeYAML = "org.yaml" % "snakeyaml" % "1.12"

  def scalatest(version: String) = {
    def st(scalaVersion: String, projectVersion: String) =
      "org.scalatest" % ("scalatest_" + scalaVersion) % projectVersion % "test"

    version match {
      case "2.8.0" => st("2.8.1", "1.8") // argh, there is no 2.8.0 scalatest version in any maven repository
      case "2.8.1" | "2.8.2" => st(version, "1.8")
      case v if v.startsWith("2.9.")  => st(version, "1.9.1")
      case v if v.startsWith("2.10.") => st("2.10", "1.9.1")
      case _ => sys.error("ScalaTest not supported for scala version %s!" format version)
    }
  }

  def coreDeps(version: String) = Seq(snakeYAML)
  def testDeps(version: String) = Seq(scalatest(version))
}

object ScalaFaker extends Build {

  def sourceDir(version: String) = version match {
    case "2.8.0" => "-2.8.0"
    case v if v.startsWith("2.10.") => ""
    case _ => "-2.8.1-2.9"
  }

  def additionalCompilerOptions(version: String): Seq[String] = version match {
    case v if v.startsWith("2.10.") => Seq("-feature")
    case _ => Seq.empty
  }

  val projectName = "scala-faker"

  val buildSettings = Defaults.defaultSettings ++ releaseSettings ++ ls.Plugin.lsSettings ++
    Seq(
      sbtPlugin := false,
      organization := "it.justwrote",
      name := projectName,
      scalaVersion := "2.10.2",
      crossScalaVersions := Seq("2.8.0", "2.8.1", "2.8.2", "2.9.0", "2.9.0-1", "2.9.1", "2.9.2", "2.10.2"),
      publishArtifact in (Compile, packageDoc) := false,
      scalacOptions ++= Seq("-deprecation", "-Xcheckinit", "-encoding", "utf8", "-g:vars", "-unchecked", "-optimize"),
      parallelExecution := true,
      parallelExecution in Test := true,
      publishTo <<= version {
        v => Some(Resolver.file("repo", file("/home/ds/Projects/justwrote.github.com/" + { if (v.trim.endsWith("SNAPSHOT")) "snapshots" else "releases" } )))
      },
      homepage := Some(new java.net.URL("https://github.com/justwrote/scala-faker/")),
      (LsKeys.tags in LsKeys.lsync) := Seq("test", "fake", "faker"),
      (externalResolvers in LsKeys.lsync) := Seq("justwrote" at "http://repo.justwrote.it/releases/"),
      description := "A library for generating fake data.",
      (LsKeys.docsUrl in LsKeys.lsync) := Some(new java.net.URL("https://github.com/justwrote/scala-faker#readme")),
      licenses := Seq(("Apache2", new java.net.URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))),
      initialCommands := "import faker._"
    )

  lazy val scalaFaker = Project(
    projectName,
    file("."),
    settings = buildSettings ++ Seq(
      libraryDependencies <<= (scalaVersion, libraryDependencies) {
        (sv, deps) => deps ++ Dependencies.coreDeps(sv) ++ Dependencies.testDeps(sv)
      },
      scalaSource in Compile <<= (scalaVersion, scalaSource in Compile) {
        (sv, source) => file(source.absolutePath + sourceDir(sv))
      },
      scalacOptions <<= (scalaVersion, scalacOptions) {
        (sv, options) => options map (_ ++ additionalCompilerOptions(sv))
      }
    )
  )
}
