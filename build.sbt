val Http4sVersion = "0.21.8"
val CirceVersion = "0.13.0"
val Specs2Version = "4.10.5"
val LogbackVersion = "1.2.3"
val SlickVersion = "3.3.3"
val ZioVersion = "1.0.3"

enablePlugins(JavaServerAppPackaging)

lazy val root = (project in file("."))
  .settings(
    name := "tevent",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.3",
    libraryDependencies ++= Seq(
      "org.http4s"            %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"            %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"            %% "http4s-circe"        % Http4sVersion,
      "org.http4s"            %% "http4s-dsl"          % Http4sVersion,
      "io.circe"              %% "circe-generic"       % CirceVersion,
      "org.specs2"            %% "specs2-core"         % Specs2Version % Test,
      "ch.qos.logback"        %  "logback-classic"     % LogbackVersion,
      "com.typesafe.slick"    %% "slick"               % SlickVersion,
      "com.typesafe.slick"    %% "slick-hikaricp"      % SlickVersion,
      "dev.zio"               %% "zio"                 % ZioVersion,
      "dev.zio"               %% "zio-test"            % ZioVersion  % Test,
      "dev.zio"               %% "zio-test-sbt"        % ZioVersion  % Test,
      "dev.zio"               %% "zio-test-intellij"   % ZioVersion  % Test,
      "dev.zio"               %% "zio-interop-cats"    % "2.2.0.1",
      "dev.zio"               %% "zio-logging"         % "0.5.3",
      "org.scalameta"         %% "svm-subs"            % "20.2.0",
      "org.scalatest"         %% "scalatest"           % "3.2.0" % Test,
      "org.scalamock"         %% "scalamock"           % "4.4.0" % Test,
      "com.github.pureconfig" %% "pureconfig"          % "0.14.0",
      "com.github.t3hnar"     %% "scala-bcrypt"        % "4.3.0",
      "com.h2database"        %  "h2"                  % "1.4.200",
      "com.github.daddykotex" %% "courier"             % "3.0.0-M2"
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings",
)
