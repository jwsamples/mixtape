import sbt._

object Dependencies {

	lazy val catsVersion = "2.1.1"
	lazy val catsEffectVersion = "2.1.1"
	lazy val circeVersion = "0.13.0"
	lazy val fs2Version = "2.2.2"
  lazy val monixVersion = "3.1.0"
  lazy val monocleVersion = "2.0.2"
	lazy val scalaTestVersion = "3.0.5"

 lazy val catsRelated = Seq(
    "org.typelevel" %% "cats-core"
  ).map(_ % catsVersion)

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion

  lazy val circeRelated = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-fs2",
    "io.circe" %% "circe-parser",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-generic-extras"
  ).map(_ % circeVersion)

 lazy val fs2Related = Seq(
    "co.fs2" %% "fs2-core",
   "co.fs2" %% "fs2-io"
  ).map(_ % fs2Version)

  lazy val loggingRelated = Seq(
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )

  lazy val monix = "io.monix" %% "monix" % monixVersion

  lazy val monocleRelated = Seq(
    "com.github.julien-truffaut" %% "monocle"
  ).map(_ % monocleVersion)

  lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.2"
}
