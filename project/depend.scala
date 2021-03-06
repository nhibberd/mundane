import sbt._
import Keys._

object depend {
  val scalaz = Seq(  "org.scalaz"           %% "scalaz-core"       % "7.1.0"
                   , "org.scalaz"           %% "scalaz-concurrent" % "7.1.0"
                   , "org.scalaz"           %% "scalaz-effect"     % "7.1.0")
  val scopt  = Seq(  "com.github.scopt"     %% "scopt"             % "3.2.0")
  val joda   = Seq(  "joda-time"            %  "joda-time"         % "2.2"
                   , "org.joda"             %  "joda-convert"      % "1.1")
  val bits   = Seq(  "org.typelevel"        %% "scodec-bits"       % "1.0.0")

  val stream = Seq(  "org.scalaz.stream"    %% "scalaz-stream"     % "0.5a")

  val specs2 = Seq(  "org.specs2"           %% "specs2-core"
                   , "org.specs2"           %% "specs2-scalacheck"
                   , "org.specs2"           %% "specs2-junit").map(_ % "2.4.5" % "test")

  val specs2Extra = Seq("org.specs2"         %% "specs2-matcher-extra" % "2.4.5" % "test" excludeAll ExclusionRule(organization = "org.scalamacros"))

  val caliper = Seq("com.google.caliper"   %  "caliper"         % "0.5-rc1",
                    "com.google.guava"     %  "guava"           % "14.0.1" force())
  val disorder =
    Seq("com.ambiata" %% "disorder" % "0.0.1-20150317050225-9c1f81e" % "test")

  def reflect(version: String) =
    Seq("org.scala-lang" % "scala-compiler" % version, "org.scala-lang" % "scala-reflect" % version) ++
      (if (version.contains("2.10")) Seq(
        compilerPlugin("org.scalamacros" %% "paradise" % "2.0.0" cross CrossVersion.full),
        "org.scalamacros" %% "quasiquotes" % "2.0.0") else Seq())

  def parboiled(sv: String) =
    if (sv.contains("2.11")) Seq(
        "org.scala-lang"      % "scala-reflect"   % sv
      , "org.parboiled"       %% "parboiled"      % "2.0.0"
      )
    else Seq(
        "org.parboiled"       %% "parboiled"      % "2.0.0"
      )

  val rng =      Seq("com.nicta"            %% "rng"            % "1.3.0")

  val kiama =    Seq("com.googlecode.kiama" %% "kiama"          % "1.6.0")

  val simpleCsv  = Seq("net.quux00.simplecsv" % "simplecsv" % "2.0")

  val resolvers = Seq(
      Resolver.sonatypeRepo("releases")
    , Resolver.typesafeRepo("releases")
    , Resolver.url("ambiata-oss", new URL("https://ambiata-oss.s3.amazonaws.com"))(Resolver.ivyStylePatterns)
    , "Scalaz Bintray Repo"   at "http://dl.bintray.com/scalaz/releases")
}
