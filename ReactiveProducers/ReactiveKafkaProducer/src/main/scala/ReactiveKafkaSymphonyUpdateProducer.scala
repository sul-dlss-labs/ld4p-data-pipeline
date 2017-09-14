
import java.nio.file.{FileSystems, Files}
import java.text.SimpleDateFormat
import java.util.Calendar

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Producer
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.{ActorMaterializer, scaladsl}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import configs.syntax._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

object ReactiveKafkaSymphonyUpdateProducer extends App {

  import akka.stream.scaladsl.{Framing, _}

  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val config = ConfigFactory.load()
  val bootstrapServers = config.getOrElse("bootstrapServers", "").toOption.fold("")(identity(_))
  val symphonyHost = config.getOrElse("symphonyHost", "").toOption.fold("")(identity(_))
  val symphonyHistLog = config.getOrElse("symphonyHistLog", "").toOption.fold("")(identity(_))

  val today = Calendar.getInstance().getTime()
  val month = new SimpleDateFormat("MM").format(today)
  val day = new SimpleDateFormat("dd").format(today)
  val year = new SimpleDateFormat("yyyy").format(today)

  val histLogPath = s"${symphonyHistLog}/${year}${month}${day}.hist"

  println(s"Using bootstrap servers: ${bootstrapServers}")
  println(s"Fetching records from: ${histLogPath}")
  println(s"Dumping records from ${symphonyHost}")

  val producerSettings = ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
    .withBootstrapServers(bootstrapServers)

  val marcFlow: Flow[ByteString, ByteString, NotUsed] = Framing.delimiter(ByteString(29.asInstanceOf[Char]),
    maximumFrameLength = 10000, allowTruncation = true)

  val recordFlow = Flow[ByteString].mapAsyncUnordered(16) { elem =>
    Future {
      ProducerMessage.Message(new ProducerRecord[String, Array[Byte]]
      ( "marc21", null, (elem ++ ByteString(29.asInstanceOf[Char])).toArray), null)
    }
  }

  val fs = FileSystems.getDefault

  val exists = Files.exists(fs.getPath(histLogPath))

  if (exists) {

    val lines: Source[String, NotUsed] = FileTailSource.lines(
      fs.getPath(histLogPath), maxLineSize = 8192, pollingInterval = 250.millis
    )

    lines.groupedWithin(500, 1 second).flatMapConcat { logLine => Source(logLine.distinct) }
      .map( e =>
        ByteString((s"ssh -K sirsi@${symphonyHost} /s/SUL/Bin/LD4P/catDumpUpdate.sh '${e}'".lineStream)(0))
      ).via(marcFlow).async.via(recordFlow).via(Producer.flow(producerSettings)).map { result =>
        val record = result.message.record
        println(s"Posted message ${record.value} to kafka ${record.topic} topic")
        result
    }.runWith(Sink.ignore)
  }
  else
    println(s"Hist Log file ${histLogPath} does not exist")

  StdIn.readLine()
  system.terminate()

}