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

Unfortunately the BananaRDF artifacts for the version of Scala we use are not available online.
Hence Banana RDF needs to be cloned, compiled and published locally before compiling this project.

```sh
git clone https://github.com/banana-rdf/banana-rdf
cd  banana-rdf
sbt ++2.11.8 publishLocal
```

## Installation

These steps walk through setting up a development environment for the `ReactiveKafkaWriter` module primarily, but should be generalizable to other Scala modules in this codebase.

### 1. Download the Project

Clone this [ld4p-data-pipeline](https://github.com/ld4p-data-pipeline) git repository.

### 2. Use `sbt` to compile/assemble

SBT resolves dependencies, compiles Java byte code and builds jar files:
- Built on part of Maven, but extended to be more interactive
- Sometimes dependencies are listed and managed by sbt, sometimes expected to be provided
- Libraries with distribution on Spark cluster part?
  - Some folks package those all together, run trick to remove certain things, then submits whole thing (this is what you see in SBT)

`assembly` builds subproject jar(s). From top level, without subproject set, it compiles everything, assembles for each project (so you probably want to switch into the individual subproject you are working on first).  Assembly artifacts could be produced to anywhere and project can utilize hierarchy in the project listing, if desired.

Moving around SBT is not like filesystem directory. Example usage:
```bash
$ projects # lists all projects available in the environment
$ project  # shows the current project
$ project ReactiveKafkaWriter # sets the sbt console current project to ReactiveKafkaWriter (or whatever)
$ compile  # compiles the current project (ReactiveKafkaWriter)
$ assembly # builds the über jar for your current project
$ tasks    # shows tasks available; same conventions as with Maven
```

### 3. Have Kafka & Spark Running Locally

#### Kafka
- [Download Kafka](https://kafka.apache.org/downloads) (or via homebrew on Mac: `brew install kafka`)
  - [Kafka Quickstart Guide](https://kafka.apache.org/quickstart)
- Configure your shell's `.profile` as needed (export `KAFKA_HOME`, update `PATH`)
- Start Kafka, Zookeeper
  - If using Homebrew, can use `brew services start zookeeper` / `brew services start kafka`
  - Alternatively, you can (based on where you installed previously):
    - start ZooKeeper: `bin/zookeeper-server-start.sh config/zookeeper.properties`
    - start Kafka: `bin/kafka-server-start.sh config/server.properties`

Right now, there is no Kafka GUI, but you can still use the CLI tools, located in `/usr/bin` where you installed Kafka.

To check if ZooKeeper is running and OK:
- ```$ echo 'ruok' | nc localhost 2181``` should return `imok`

#### Spark
Manual installation:
- [Download Spark 2.2.0 (for Hadoop 2.7)](https://spark.apache.org/downloads.html)
- Configure your shell's `.profile` (export `SPARK_HOME`, update `PATH`)

Sbin within script: `sbin/start-master.sh` (dependent on where you installed Spark locally)
- Then you can see [localhost:8080](localhost:8080) for the Spark dashboard (to see Spark Master)
- There is also an `sbin/start-all.sh` to start the master and "agent" (slave) systems.

About Spark cluster management / cluster environment:
- Master node controls the rest (Master process, Worker processes)
  - On the worker process you launch executors, and parallelization occurs there
- Executor nodes (multiple) run the job(s)
  - Where applications run, executors are executed by workers
- Driver is your application, where you write the code, can be run wherever (another executor node)
  - it sends work to Executors
  - Has implications on how data moves around depending for the operation you’re running
- `start-slave` doesn’t start unless you point to something, but `start-all` doesn’t require that

### 4. Launch the Application
Deploy the App to Spark, e.g.:
```sh
spark-submit --class EstimatorStreamingApp --name EstimatorStreamingApp --master spark:/SPARK-MASTER-URL.local:7077 --deploy-mode cluster --executor-memory 14G --num-executors 2
```

You can retrieve the `SPARK-MASTER-URL` (and Slave Node URLs) from the GUI.

Note on Local / local-cluster / cluster options: deploy-mode has nothing to do with how you run the application, its only for launching the application.

Then start the application via your generated Jar, like:
```sh
java -jar ld4p-data-pipeline/EstimatorStreamingApp/target/scala-2.11/EstimatorStreamingApp-assembly-1.0.0-SNAPSHOT.jar
```

Depending on where you installed, you might need to configure logging or use `sudo` to allow logs to be written.

### 5. Create a Kafka Topic for the MARC

There are options for auto-creating topics in Kafka when that topic is written to. However, there is an error of Kafka auto creating that topic when Spark was looking to create, so it is recommended to keep that option disabled.

These steps start kafka, create a topic named `marc21` and display info on the created topic, respectively:
```sh
kafka-server-start.sh -daemon ~/Dev/kafka/config/server.properties
kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 16 --topic marc21
kafka-topics.sh --describe --zookeeper localhost:2181 --topic marc21
```

FYI: Deleting topics doesn’t actually delete the topic, only marks it as "deleted". In Kafka `server.properties` file, near the top, this behavior can be changed ("delete topic enable"), if needed.

### 6. Grab the MARC21 data in structured directories

Need data files and a given directory structure.  First, be on the Stanford VPN.  Then:
```bash
mkdir -p ~/Dev/data
pushd ~/Dev/data
kinit
scp ld4p@sul-ld4p-converter-dev.stanford.edu/ld4pData.1.zip ./
unzip ld4pData.1.zip
popd
```

Note: This data is needed identically on each machine for clusters to work from filesystem data.

#### `dataDir`

`dataDir`, the location where filesystem data is read, is set either through `application.conf` file(s) or ENV variable.
Default value: `${HOME}/Dev/data/ld4pData`.  If you use a different location, adjust your conf files or ENV accordingly.

Currently, some original structure is expected underneath that, as provided by the zip file.

### 7. ReactiveKafkaWriter is ready to be run

The class is configured to read from the Casalini MARC data as downloaded & configured above (36k records).

- From CLI: `java -jar ReactiveKafkaWriter/target/scala-2.11/ReactiveKafkaWriter-assembly-1.0.0-SNAPSHOP.jar`
- Then in the GUI, should see the activity:
  - Spark Job input (you can see the Spark GUI to see the data processing)
  - Kafka Manager for view (optional separate application to set up; not set up here)

### 8. How to kill a Spark job:
To stop properly:
- Stop master (spark jobs)
- Stop slave (spark jobs)

When restarting the Spark streaming app:
- Reads the data from Kafka, doesn’t care about where the data is
- Application is done in such a way that it doesn’t read all the data from the beginning, but reads from the last offset
- Start spark streaming, spark reading

### 9. Stop / Shut down your Kafka and Zoopeer instances

If using homebrew, can run `brew services stop kafka` then `brew services stop zookeeper`

### 10. Deployment on Amazon via Capistrano

Provision AWS systems, e.g. use
- https://github.com/darrenleeweber/aws-ops
  - generic utilities for AWS EC2 and services (ZooKeeper, Kafka)
- https://github.com/sul-dlss/spark-ec2
  - https://github.com/sul-dlss/ld4p-data-pipeline/wiki/Provision-Spark-on-Amazon-EC2

Once the AWS systems are available, setup `~/.ssh/config` and `/etc/hosts`, e.g.

```
# /etc/hosts
{aws_public_ip}  ld4p_dev_spark_master
{aws_public_ip}  ld4p_dev_spark_worker1
# plus any additional worker nodes
```

```
# ~/.ssh/config

Host ld4p_dev_spark_master
    User {aws_user}
    Hostname {use /etc/hosts name}
    IdentityFile ~/.ssh/{key-pair}.pem
    Port 22

Host ld4p_dev_spark_worker1
    User {aws_user}
    Hostname {use /etc/hosts name}
    IdentityFile ~/.ssh/{key-pair}.pem    
    Port 22

# plus any additional worker nodes
```

Then the usual capistrano workflow can be used, i.e.
```bash
bundle install
bundle exec cap -T
bundle exec cap ld4p_dev deploy:check
bundle exec cap ld4p_dev deploy
bundle exec cap ld4p_dev shell
```

Once the project is deployed to all the servers, run the assembly task for
a project, e.g.
```bash
bundle exec cap ld4p_dev spark:assembly
bundle exec cap ld4p_dev stardog:assembly
```

