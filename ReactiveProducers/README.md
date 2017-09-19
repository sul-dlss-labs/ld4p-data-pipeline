# Reactive Producers

There are currently two reactive producers in this project, one that dumps an entire file
of MARC records--based on a list of catalog keys--to the Kafka pipeline for processing via Spark,
and one that tails a transaction history log to extract a catalog key, dump a MARC record, and then
sends it to the Kafka pipeline for further downstream processing. The producers are (respectively):

`ReactiveKafkaDumpProducer`

`ReactiveKafkaUpdateProducer`

## Configuration and Environment

Take a look at the `src/main/resources/application.conf` file for each of the producers.
Here you will see the default variables that are set by default or by aan environment variable
on the machine that is running the producer.

*bootstrapServers* is a list of servers where the producer can connect to the Kafka pipeline.
The default value is a list of `localhost` endpoints assuming you are connecting to an instance
of Kafka on your local machine. In order to change this, set the environment variable:

```
export BOOTSTRAP_SERVERS="example-1.us-west-2.amazonaws.com:9092,example-2.amazonaws.com:9092,example-3.amazonaws.com:9092"
```

*symphonyHost* is the host where you want to run a dump or fetch continuous updates of records.
The default value is `morison.stanford.edu`. To change the default value set your environment variable:
```
export SYMPHONY_HOST="example-symphony-app-stage.stanford.edu"
```

*symphonyHistLog* is the location of the transaction history log that this application will poll to
find out current items and records that are being edited and changing in Symphony throughout the day.
The default value is the location of the transaction hostory log in Symphony (`/s/sirsi/Unicorn/Logs/Hist`).
There should be no need to change this default value when running this application on a Symphony box,
but if you are testing against a local copy of a transaction history log file. In that case you would
change the default by setting your environment variable:

```
export SYMPHONY_HISTLOG="/Users/someuser/Desktop"
```

## Running the Producers

There are several ways to run the producers, locally or on the Symphony application machine, and via SBT or using
and assembled JAR file.

### Assembling the JAR file
In order to assemble the JAR files do the following for each producer:

- In the project root directory (`ld4p-data-pipeline`) type `sbt` on the command line
- Type `project ReactiveKafkaDumpProducer` or `project ReactiveKafkaUpdateProducer` depending on which one you are compiling
- Type `assembly`
- exit SBT by typing `exit`
- Copy the file (e.g.) `/ReactiveProducers/ReactiveKafkaUpdateProducer/target/scala-2.11/ReactiveKafkaUpdateProducer-assembly-1.0.0-SNAPSHOT.jar`
to an appropriate location, for example, onto the Symphony box at `/s/SUL/Bin/LD4P`

### Running the Producers locally

There are two ways to run the producers locally: via SBT or via the assembled JAR file.

To run via SBT:

- In the project root directory (`ld4p-data-pipeline`) type `sbt` on the command line
- Type `project ReactiveKafkaDumpProducer` or `project ReactiveKafkaUpdateProducer` depending on which one you want to run
- For the Dump Producer type `run /path/to/a/file/of/catalogKeys ssh`
- For the Update Producer just type `run`

To run via the assembled JAR file:

- Follow the steps above to assembel the JAR file and navigate to the location of the JAR file
- For the Dump Producer type `java -jar ReactiveKafkaDumpProducer-assembly-1.0.0-SNAPSHOT.jar /path/to/a/remote/file/of/catalogKeys ssh`
- For the Update Producer just type `java -jar ReactiveKafkaUpdateProducer-assembly-1.0.0-SNAPSHOT.jar`

### Running the Producers on the Symphony Box

- First you must copy the JAR files to a location on the Symphony machine, for example to `/s/SUL/Bin/LD4P` and then
navigate to that location on the Symphony box
- For the Dump Producer type `java -jar ReactiveKafkaDumpProducer-assembly-1.0.0-SNAPSHOT.jar /path/to/a/file/of/catalogKeys` symphony
- For the Update Producer:
    - Make sure that there is a log file with the current day's date in the format YYYYMMdd.hist at `/s/sirsi/Unicorn/Logs/Hist/`
    - type `java -jar ReactiveKafkaUpdateProducer-assembly-1.0.0-SNAPSHOT.jar` symphony
    - If you will be running this continuously throughout the day you may want to run this as a background job using `nohup` and
    directing stdout and stderr to a log file.

The 'synphony' argument instructs the program that it is running on the remote box and to use paths appropriatly without SSH.

## A note about the Producer arguments and Symphony Authentication

The last argument when invoking the Dump producer locally may be nothing, or 'ssh'. If there is no last argument of 'ssh'
the program assumes you are running it against an already dumped MARC file that exists on your machine locally.
In that case you would invoke it as:
`run /path/to/a/file/of/dumpedMARCRecords`. The 'ssh' argument instructs the program to SSH onto the Symphony box to run
a script that will perform a catalogdump against a file of ckeys that is on the Symphony box. So you must supply a path to
a file of ckeys on the remote box.
If you are running the producers locally with the SSH option you must otherwise be able to SHH onto the Symphony box. This normally
requires that you be listed in the `.k5login` file on the remote box and that you have current Kerberos (GSSAPI) credentials.
