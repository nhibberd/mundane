libraryDependencies ++= Seq(
    "com.nicta"              %% "rng"                % "1.1.1",
    "org.scalaz"             %% "scalaz-core"        % "7.0.4",
    "joda-time"              %  "joda-time"          % "2.1",
    "org.joda"               %  "joda-convert"       % "1.1",
    "com.googlecode.kiama"   %% "kiama"              % "1.5.1",
    "commons-io"             %  "commons-io"         % "2.4",
    "org.specs2"             %% "specs2-matcher"     % "2.3.4" % "optional",
    "org.scalacheck"         %% "scalacheck"         % "1.11.1" % "optional"
  )

libraryDependencies ++= Seq(
    "com.ambiata"         %% "scrutiny"             % "1.1-20131224033803-9095a20",
    "org.specs2"          %% "specs2-matcher-extra" % "2.3.4" ,
    "org.specs2"          %% "specs2-core"          % "2.3.4" ,
    "org.specs2"          %% "specs2-junit"         % "2.3.4" ,
    "org.specs2"          %% "specs2-scalacheck"    % "2.3.4" ).map(_ % "test")


resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.typesafeRepo("releases"),
  "artifactory"           at "http://etd-packaging.research.nicta.com.au/artifactory/libs-release-local")
