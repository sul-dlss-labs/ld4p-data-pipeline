import java.nio.file.{FileSystems, Path}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{Flow, Sink, Source}
import better.files.File
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import configs.syntax._

object ReactiveKafkaFsProducer extends App {

  println(s"Starting ${getClass.getName} ...")

  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher
  val MAX_ALLOWED_FILES     = 1000
  val config                = ConfigFactory.load()
  val dir                   = config.getOrElse("dataDir", "").toOption.fold("")(identity(_))

  val path                  = File(s"${dir}/Casalini_mrc").path.toString

  val bootstrapServers = config.getOrElse("bootstrapServers", "").toOption.fold("")(identity(_))

  val producerSettings = ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
    .withBootstrapServers(bootstrapServers)
  
  val source: Source[Path, NotUsed] = Directory.ls(FileSystems.getDefault.getPath(path))

  val readflow  = Flow[Path].mapAsyncUnordered(16){ e => Future{ (e.getFileName.toString, File(e.toAbsolutePath).byteArray) } }

  val writeflow = Flow[(String, Array[Byte])].mapAsyncUnordered(16) { elem =>
      Future {new ProducerRecord[String, Array[Byte]]("marc21", elem._1, elem._2)}
    }

  val done = source.async.via(readflow).async.via(writeflow).runWith(Producer.plainSink(producerSettings))

  done.onComplete {
    case Success(e) => println("file copied with success"); system.terminate()
    case Failure(e) => println(s"process ended with failure: ${e.getMessage}"); system.terminate()
  }

}
