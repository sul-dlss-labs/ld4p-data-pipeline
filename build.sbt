
def ld4pProjects(pathName: String): Project = (Project(pathName.split("/").last, file(pathName)))

lazy val commonSettings = Seq (
  organization:= "edu.stanford.library",
  version := "1.0.0-SNAPSHOT",
  scalaVersion := "2.11.11",
  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.3.1",
    "com.github.kxbmap" %% "configs" % "0.4.4",
    "nl.grons" %% "metrics-scala" % "3.5.9_a2.3",
    "org.scalatest" %% "scalatest" % "3.0.1" % Test
  ),
  resolvers += "bblfish-snapshots" at "http://bblfish.net/work/repo/releases"
  // If you want to run with Provided dependency
  // run in Compile := Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)).evaluated
)

lazy val `ld4p-data-pipeline` = (project in file("."))
  .settings(commonSettings)
  .aggregate(sparkStreamingConvertors, reactiveConsumers, reactiveProducers,
    tools, demos
  )

val sparkStreamingConvertorsName  = "SparkStreamingConvertors"
lazy val sparkStreamingConvertors = ld4pProjects(sparkStreamingConvertorsName).aggregate(m21toBibFDumpConvApp, m21toBibFContinousConvApp)

val consumersProjectName   = "ReactiveConsumers"
lazy val reactiveConsumers = ld4pProjects(consumersProjectName).aggregate(ReactiveKafkaConsumer)

val producerProjectName    = "ReactiveProducers"
lazy val reactiveProducers = ld4pProjects(producerProjectName).aggregate(ReactiveKafkaDumpProducer, ReactiveKafkaUpdateProducer)

val toolProjectName        = "Tools"
lazy val tools             = ld4pProjects(toolProjectName).aggregate(AkkaStreamMarcReader)

val demoProjectName        = "Demos"
lazy val demos             = ld4pProjects(demoProjectName).aggregate(estimator, estimatorStreaming, marcXMLtoBibFrame, ReactiveFolderCopier, ReactiveFolderReader, singleMetricExample)

/**
  *  The Concrete Projects Applications
  */

lazy val m21toBibFDumpConvApp = ld4pProjects(sparkStreamingConvertorsName + "/M21toBibFDumpConvApp")
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.marc4j" % "marc4j" % "2.8.2",
      "org.apache.spark" % "spark-streaming-kafka-0-10_2.11" % "2.2.0",
      "org.apache.spark" % "spark-core_2.11" % "2.2.0",
      "org.apache.spark" % "spark-streaming_2.11" % "2.2.0",
      "net.sf.saxon" % "Saxon-HE" % "9.7.0-20",
      "com.typesafe.akka" %% "akka-stream" % "2.5.4",
      "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
      "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.11",
      "com.github.benfradet" %% "spark-kafka-writer" % "0.4.0",
      "org.apache.spark" %% "spark-sql" % "2.0.2",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.4" % Test
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    mainClass in assembly := Some("M21toBibFDumpConvApp")
  )

lazy val m21toBibFContinousConvApp = ld4pProjects(sparkStreamingConvertorsName + "/M21toBibFContinousConvApp")
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.marc4j" % "marc4j" % "2.8.2",
      "org.apache.spark" % "spark-streaming-kafka-0-10_2.11" % "2.2.0",
      "org.apache.spark" % "spark-core_2.11" % "2.2.0",
      "org.apache.spark" % "spark-streaming_2.11" % "2.2.0",
      "net.sf.saxon" % "Saxon-HE" % "9.7.0-20",
      "com.typesafe.akka" %% "akka-stream" % "2.5.4",
      "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
      "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.11",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.4" % Test
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    mainClass in assembly := Some("M21toBibFContinousConvApp")
)

// Simple function to help pick banana dependency. Nothing fency
val banana = (name: String) => "org.w3" %% name % "0.8.4" excludeAll (ExclusionRule(organization = "org.scala-stm"))

lazy val ReactiveKafkaConsumer = ld4pProjects(consumersProjectName + "/ReactiveKafkaConsumer")
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % "2.5.4",
      "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
      "com.github.pathikrit" %% "better-files" % "2.17.1",
      "org.marc4j" % "marc4j" % "2.8.2"
    ),
    libraryDependencies ++= Seq("banana", "banana-rdf", "banana-jena").map(banana),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case "application.conf" => MergeStrategy.concat
      case "reference.conf"   => MergeStrategy.concat
      case x => MergeStrategy.first
    },
    mainClass in assembly := Some("ReactiveKafkaStardogConsumer")
  )

lazy val ReactiveKafkaDumpProducer = ld4pProjects(producerProjectName + "/ReactiveKafkaProducer")
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % "2.5.4",
      "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
      "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.11",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.4" % Test,
      "com.github.pathikrit" %% "better-files" % "2.17.1"
    ),
    mainClass in assembly := Some("ReactiveKafkaSymphonyProducer")
  )

lazy val ReactiveKafkaUpdateProducer = ld4pProjects(producerProjectName + "/ReactiveKafkaProducer")
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % "2.5.4",
      "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
      "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.11",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.4" % Test,
      "com.github.pathikrit" %% "better-files" % "2.17.1"
    ),
    mainClass in assembly := Some("ReactiveKafkaSymphonyProducer")
  )
/**
  *  Tools & Demos
  */

lazy val estimator = ld4pProjects(demoProjectName + "/EstimatorApp")
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

lazy val estimatorStreaming = ld4pProjects(demoProjectName + "/EstimatorStreamingApp")
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.marc4j" % "marc4j" % "2.8.2",
      "org.apache.spark" % "spark-streaming-kafka-0-10_2.11" % "2.2.0",
      "org.apache.spark" % "spark-core_2.11" % "2.2.0",
      "org.apache.spark" % "spark-streaming_2.11" % "2.2.0",
      "net.sf.saxon" % "Saxon-HE" % "9.7.0-20"
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    mainClass in assembly := Some("EstimatorStreamingApp")
  )

lazy val marcXMLtoBibFrame = ld4pProjects(demoProjectName + "/MarcXMLtoBibFrame")
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

lazy val ReactiveFolderCopier = ld4pProjects(demoProjectName + "/ReactiveFolderCopier")
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % "2.5.4",
      "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.11",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.4" % Test,
      "com.github.pathikrit" %% "better-files" % "2.17.1",
      "org.marc4j" % "marc4j" % "2.8.2"
    ),
    mainClass in assembly := Some("ReactiveFolderCopier")
  )

lazy val ReactiveFolderReader = ld4pProjects(demoProjectName + "/ReactiveFolderReader")
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % "2.5.4",
      "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.11",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.4" % Test,
      "com.github.pathikrit" %% "better-files" % "2.17.1"
    ),
    mainClass in assembly := Some("ReactiveFolderReader")
  )

lazy val singleMetricExample = ld4pProjects(demoProjectName + "/SingleMetricExample")
  .settings(
    commonSettings,
    mainClass in assembly := Some("SingleMetricExample")
  )

/**
  * Tools
  */
lazy val AkkaStreamMarcReader = ld4pProjects(toolProjectName + "/AkkaStreamMarcReader")
  .settings(
    commonSettings,
    libraryDependencies ++= Seq (
      "com.typesafe.akka" %% "akka-stream" % "2.5.4",
      "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.11",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.4" % Test,
      "com.github.pathikrit" %% "better-files" % "2.17.1",
      "org.marc4j" % "marc4j" % "2.8.2"
    ),
    mainClass in assembly := Some("AkkaStreamMarcReader")
  )
