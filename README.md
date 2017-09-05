[![Build Status](https://travis-ci.org/sul-dlss/ld4p-data-pipeline.png?branch=master)](https://travis-ci.org/sul-dlss/ld4p-data-pipeline)

# ld4p-data-pipeline

Project Management : https://ld-stanford.myjetbrains.com/youtrack/

## Requirements

(This is in progress)

- `Scala`: 2.11.11
- `Scala Build Tool`
- `Kafka`: 0.11.x
- `Spark`: 2.2.0, Pre-built for Apache Hadoop 2.7
- `BananaRDF` (see note below on installing)

The Project depends on BananaRDF. Unfortunately the banana artifacts for the version of Scala we use are not available online.
Hence Banana RDF needs to be cloned, compiled and published locally before compiling this project.

```sh
git clone https://github.com/banana-rdf/banana-rdf
cd  banana-rdf
sbt ++2.11.8 publishLocal
```

## Installation

These steps walk through setting up a development environment for the `ReactiveKafkaWriter` module primarily, but should be generalizable to other Scala modules in this codebase.

1. Download the Project:
  - Clone this [ld4p-data-pipeline](https://github.com/ld4p-data-pipeline) git repository
2. Compile the Project: Start SBT in console (```$ sbt ```) and in the SBT console:  
  - SBT dependencies:
    - Sometimes listed, sometimes provided
    - Libraries with distribution on Spark cluster part?
    - Normally, supposed to put dependencies provided (Spark has libraries, but doesn’t work)
      - Some folks package those all together, run trick to remove certain things, then submits whole thing (this is what you see in SB5)
  - Resolves the dependencies first
  - Assembly builds über jar
    - Built on part of Maven, but extended to be more interactive
    - From top level, without project set, compiles everything, assembles for each project (don’t run Assembly from the top level)
  - Moving around the SBT file:
    - Not like bash / directory. Type projects to get around
    - Codebase is currently flat. Can put projects where you want
    - Order right now is logical, not physical
    - Assembly could end up anywhere?
        - Can physically decide to place them where you want
        - At same level, can have multi-level project
        - Hierarchy is in the project listing
  ```
  $ projects # lists all projects available in the environment
  $ project # shows the current project
  $ project ReactiveKafkaWriter # sets the sbt console current project to ReactiveKafkaWriter (or whatever)
  $ compile # compiles the current project
  $ assembly # builds the über jar for your current project
  $ tasks # shows tasks available ; same conventions as with Maven
  ```
3. Have Kafka & Spark Running Locally
  - Can use package manager versions, then the project itself will clone that
  - **Kafka:**
    - [Download Kafka](https://kafka.apache.org/downloads) (or can use homebrew on Mac)
      - [Kafka Quickstart Guide](https://kafka.apache.org/quickstart)
    - Set up configurations for your shell's `.profile` as needed (`KAFKA_HOME`, update `PATH`)
    - Start Kafka, Zookeeper
      - You have Zookeeper inside of Kafka
      - Daniel recommends using Ubuntu distribution zookeeper, not the Kafka distribution zookeeper
      - If using Homebrew, can use `brew services start zookeeper` / `brew services start Kafka`
      - Alternatively, you can:
        - start ZooKeeper: `bin/zookeeper-server-start.sh config/zookeeper.properties` (change to where you installed Kafka/ZooKeeper)
        - start Kafka: `bin/kafka-server-start.sh config/server.properties` (change to where you installed Kafka)
          - So for Daniel's setup: `sudo /opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/server.properties`
          - Kafka start up also has a deamon mode.
    - Right now, there is no Kafka GUI (can still use the CLI tools, located in `/usr/bin` where you installed Kafka)
    - To check if ZooKeeper is running and OK:
        - ```$ echo 'ruok' | nc localhost 2181``` should return `imok`
  - **Spark:**
    - Manual installation:
      - Config for .profile SPARK_HOME, JAVA_HOME, add all of these to $PATH
      - Go to spark.apache.org
      - Download the binary for "2.2.0, Pre-built for Apache Hadoop 2.7"
    - Sbin within script: `sbin/start-master.sh` (dependent on where you installed Spark locally)
      - Then you can see localhost:8080 for the Spark dashboard (to see Spark Master)
      - There is also an `sbin/start-all.sh` to start the master and “agent” (slave) systems.
    - About Spark cluster management / cluster environment:
      - Master node controls the rest
        - Master process
        - Worker process
        - On the worker process you launch executors, and parallelization occurs there
      - Executor nodes (multiple) run the job(s)
        - Where applications run
        - Executors are executed by workers
      - Driver is your application, where you write the code
        - One that sends work to Executors
        - Driver is on another executor node
        - Your program; Driver is the program that can be run wherever
        - Has implications on how data moves around depending for the operation you’re running
      - Start slave doesn’t start unless you point to something
          - Start-all doesn’t require that
4. Launch the Application
  - Deploy the App to Spark: `spark-submit --class EstimatorStreamingApp --name EstimatorStreamingApp --master spark:/SPARK-MASTER-URL.local:7077 --deploy-mode cluster --executor-memory 14G --num-executors 2`
    - `Spark-job —class Class of App —name Name of App —master master node URL —deploy-mode cluster (still even running locally) — executor-memory 14G (if not available, will use less) —num-executors`
    - you can retrieve the Spark Master and Slave Node URLs from the GUI
    - Local / local-cluster / cluster options: deploy-mode has nothing to do with how you run the application, its only for launching the application.
  - Start the Application via the generated Jar: `.../ld4p-data-pipeline/EstimatorStreamingApp/target/scala-2.11/EstimatorStreamingApp-assembly-1.0.0-SNAPSHOT.jar`
    - change this to wherever you downloaded the codebase and built the Jar
    - For Darren, had a permission issue on logging for the app, so used `sudo`, e.g.
      - `sudo /opt/spark/bin/spark-submit --class EstimatorStreamingApp --name EstimatorStreamingApp --master spark://sul-dlweber-ubuntu:7077  --deploy-mode cluster --executor-memory 6G --num-executors 1`
      - `/data/src/dlss/ld4l/ld4p-data-pipeline/EstimatorStreamingApp/target/scala-2.11/EstimatorStreamingApp-assembly-1.0.0-SNAPSHOT.jar`
5. Create a Kafka Topic for the MARC
  - Need to create a Kafka topic for MARC21
    - There are options for auto-creating topics in Kafka when that topic is written to
    - However, there is an error of Kafka auto creating that topic when Spark was looking to create
  - Start kafka: `kafka-server-start.sh -daemon ~/Dev/kafka/config/server.properties`
  - Create the topic: `kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 16 --topic marc21`
  - Describe the topic to check it is correct: `kafka-topics.sh --describe --zookeeper localhost:2181 --topic marc21`
    - Should have 16 partitions
  - FYI: Deleting topics: doesn’t actually delete the topic
    - In Kafka `server.properties` file, near the top, commented out but for test/dev environments, edit that file and uncomment that disable deletion of topics ("delete topic enable")
6. Grab the MARC21 data & Set in the Appropriate Space for your Application
  - Need the files as structure in directory from Daniel
      - Daniel will put the data here: `ld4p@sul-ld4p-converter-dev.stanford.edu:~/data/casalini?`
      - The structure: should have 3 folders `casalini?` with all the individual files below those directories.
  - Running the program:
      - Set `dataDir` (either through path or env variable) then make sure inside of that dataDir path it has the original structure
      - Open question from Atz: Configure this to point to the data you want so there isn’t a dependency on the data structure?
      - Example from Daniel: `{HOME}/Dev/data/ld4pData`
      - Need this data on each machine for clusters to work
7. ReactiveKafkaWriter is ready to be run
  - Configured to read from the Casalini MARC data as downloaded & configured above (36k records)
  - From CLI: `java -jar ReactiveKafkaWriter/target/scala-2.11/ReactiveKafkaWriter-assembly-1.0.0-SNAPSHOP.jar`
  - From the GUI, should see the output
      - Spark Job input (you can see the Spark GUI to see the data processing)
      - Kafka Manager for view (separate application to set up; not set up here)
  - Partitions in Kafka auto configure number of tasks executing
9. How to kill a Spark job:
  - To stop properly:
    - Stop master (spark jobs)
    - Stop slave (spark jobs)
  - When restart the Spark streaming app:
    - Reads the data from Kafka, doesn’t care about where the data is
    - Application is done in such a way that it doesn’t read all the data from the beginning, but reads from the last offset
    - Start spark streaming, spark reading
10. Stop / Shut down your Kafka and Zoopeer instances
  - If using homebrew, can run `brew services stop kafka` then `brew services stop zookeeper`
