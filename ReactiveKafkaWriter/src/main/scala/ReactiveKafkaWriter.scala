import java.nio.file.{FileSystems, Path}


import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{Flow, Sink, Source}
import better.files.File
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.Future
import scala.util.{Failure, Success}


object ReactiveKafkaWriter extends App {

  println("Starting ReactiveKafkaWriter ....")

  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher
  val MAX_ALLOWED_FILES     = 1000
  val inDir                 = File("/Users/sul.maatari/IdeaProjects/Worksheet/src/spike/scala/Casalini_mrc")

  val producerSettings = ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
    .withBootstrapServers("localhost:9092, 192.168.0.101:9092, Maatari-Stanford.local:9092")

  val path                          = inDir.path.toString
  val fs                            = FileSystems.getDefault
  val source: Source[Path, NotUsed] = Directory.ls(fs.getPath(path))

  val readflow  = Flow[Path].mapAsyncUnordered(16){ e => Future{ (e.getFileName.toString, File(e.toAbsolutePath).byteArray) } }

  val writeflow = Flow[(String, Array[Byte])].mapAsyncUnordered(16) { elem =>
      Future {new ProducerRecord[String, Array[Byte]]("marc21", elem._1, elem._2)}
    }

  val done = source.async.via(readflow).async.via(writeflow).runWith(Producer.plainSink(producerSettings))

  done.onComplete {
    case Success(e) => println("file copied with success"); system.terminate()
    case Failure(e) => println("process ended with failure"); system.terminate()
  }

}
