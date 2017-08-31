//name := "ld4p-data-pipeline"

lazy val commonSettings = Seq(
  organization:= "edu.stanford.library",
  version := "1.0.0-SNAPSHOT",
  scalaVersion := "2.11.11",
  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.3.1",
    "com.github.kxbmap" %% "configs" % "0.4.4",
    "org.scalatest" %% "scalatest" % "3.0.1" % Test
  )
  //If you want to run with Provided dependency
  //run in Compile := Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)).evaluated
)

lazy val akkaSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % "2.5.4",
    "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.11",
    "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.4" % Test
  )
)

lazy val ld4pDataPipeline = (project in file("."))
  .settings(commonSettings)
  .aggregate(
    estimator, marcXMLtoBibFrame, estimatorStreaming, ReactiveKafkaConsumer,
    ReactiveKafkaWriter, AkkaStreamMarcReader, marcXMLtoBibFrame,
    ReactiveFolderCopier, ReactiveFolderReader
  )

def ld4pProjects(name: String): Project = (Project(name, file(name)))

/**
  * Core Projects
  */
lazy val estimator = ld4pProjects("EstimatorApp")
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.apache.spark" % "spark-core_2.11" % "2.2.0",
      "org.apache.spark" % "spark-streaming_2.11" % "2.2.0",
      "com.github.pathikrit" %% "better-files" % "2.17.1",
      "org.marc4j" % "marc4j" % "2.8.2",
      "net.sf.saxon" % "Saxon-HE" % "9.7.0-20"
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    mainClass in assembly := Some("EstimatorApp")
  )

lazy val estimatorStreaming = ld4pProjects("EstimatorStreamingApp")
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.apache.spark" % "spark-streaming-kafka-0-10_2.11" % "2.2.0",
      "org.apache.spark" % "spark-core_2.11" % "2.2.0",
      "org.apache.spark" % "spark-streaming_2.11" % "2.2.0",
      "org.marc4j" % "marc4j" % "2.8.2",
      "net.sf.saxon" % "Saxon-HE" % "9.7.0-20"
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    mainClass in assembly := Some("EstimatorStreamingApp")
  )

lazy val ReactiveKafkaConsumer = ld4pProjects("ReactiveKafkaConsumer")
  .settings(
    akkaSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
      "org.marc4j" % "marc4j" % "2.8.2"
    ),
    mainClass in assembly := Some("ReactiveKafkaConsumer")
  )

lazy val ReactiveKafkaWriter = ld4pProjects("ReactiveKafkaWriter")
  .settings(
    akkaSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
      "com.github.pathikrit" %% "better-files" % "2.17.1"
    ),
    mainClass in assembly := Some("ReactiveKafkaWriter")
  )

/**
  *  Utils & Demos
  */
lazy val AkkaStreamMarcReader = ld4pProjects("AkkaStreamMarcReader")
  .settings(
    akkaSettings,
    libraryDependencies ++= Seq(
      "com.github.pathikrit" %% "better-files" % "2.17.1",
      "org.marc4j" % "marc4j" % "2.8.2"
    ),
    mainClass in assembly := Some("AkkaStreamMarcReader")
  )

lazy val marcXMLtoBibFrame = ld4pProjects("MarcXMLtoBibFrame")
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.apache.spark" % "spark-core_2.11" % "2.2.0",
      "com.github.pathikrit" %% "better-files" % "2.17.1",
      "net.sf.saxon" % "Saxon-HE" % "9.7.0-20"
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    mainClass in assembly := Some("MarcXMLtoBibFrame")
  )

lazy val ReactiveFolderCopier = ld4pProjects("ReactiveFolderCopier")
  .settings(
    akkaSettings,
    libraryDependencies ++= Seq(
      "com.github.pathikrit" %% "better-files" % "2.17.1",
      "org.marc4j" % "marc4j" % "2.8.2"
    ),
    mainClass in assembly := Some("ReactiveFolderCopier")
  )

lazy val ReactiveFolderReader = ld4pProjects("ReactiveFolderReader")
  .settings(
    akkaSettings,
    libraryDependencies ++= Seq(
      "com.github.pathikrit" %% "better-files" % "2.17.1"
    ),
    mainClass in assembly := Some("ReactiveFolderReader")
  )
