val scala3Version = "3.5.0"
val PekkoVersion = "1.0.2"
val PekkoHttpVersion = "1.0.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ps_test",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test,
    libraryDependencies += "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
    libraryDependencies += "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
    libraryDependencies += "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion
    
  )
