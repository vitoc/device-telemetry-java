// Copyright (c) Microsoft. All rights reserved.

name := "telemetry"
organization := "com.microsoft.azure.iotsolutions"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  filters,
  guice,
  javaWs,
  // https://github.com/Azure/azure-documentdb-java
  "com.microsoft.azure" % "azure-documentdb" % "1.12.0"
)

// Test dependencies
libraryDependencies ++= Seq(
  // http://search.maven.org/#search%7Cga%7C1%7Cmockito-core
  "org.mockito" % "mockito-core" % "2.8.47" % "test"
)

lazy val commonSettings = Seq(
  version := "0.1.6",

  organizationName := "Microsoft Azure",
  organizationHomepage := Some(new URL("https://www.microsoft.com/internet-of-things/azure-iot-suite")),
  homepage := Some(new URL("https://www.microsoft.com/internet-of-things/azure-iot-suite")),
  startYear := Some(2017),

  // Assembly
  assemblyMergeStrategy in assembly := {
    case m if m.startsWith("META-INF") ⇒ MergeStrategy.discard
    case m if m.contains(".txt")       ⇒ MergeStrategy.discard
    case x                             ⇒ (assemblyMergeStrategy in assembly).value(x)
  },

  // Publishing options, see http://www.scala-sbt.org/0.13/docs/Artifacts.html
  licenses += ("MIT", url("https://github.com/Azure/telemetry-java/blob/master/LICENSE")),
  publishMavenStyle := true,
  publishArtifact in Test := true,
  publishArtifact in(Compile, packageDoc) := true,
  publishArtifact in(Compile, packageSrc) := true,
  publishArtifact in(Compile, packageBin) := true,

  // Test
  testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v")),

  // Misc
  logLevel := Level.Info, // Debug|Info|Warn|Error
  scalacOptions ++= Seq("-deprecation", "-explaintypes", "-unchecked", "-feature"),
  showSuccess := true,
  showTiming := true,
  logBuffered := false,
  fork := true, // required for "sbt run" to pick up javaOptions
  parallelExecution := true
)

// Main module
lazy val telemetry = project.in(file("."))
  .enablePlugins(PlayJava)
  .configs(IntegrationTest)
  .settings(commonSettings)

// Play framework
PlayKeys.externalizeResources := false

// Docker
// Note: use lowercase name for the Docker image details
enablePlugins(JavaAppPackaging)
dockerRepository := Some("azureiotpcs")
dockerAlias := DockerAlias(dockerRepository.value, None, packageName.value + "-java", Some((version in Docker).value))
maintainer in Docker := "Jill Bender (https://github.com/jillcary)"
dockerBaseImage := "toketi/openjdk-8-jre-alpine-bash"
dockerUpdateLatest := true
dockerBuildOptions ++= Seq("--squash", "--compress", "--label", "Tags=Azure,IoT,PCS,telemetry,Java")
// Example params: -Dconfig.file=/opt/conf/prod.conf -Dhttp.port=1234 -Dhttp.address=127.0.0.1
dockerEntrypoint := Seq("bin/telemetry")
