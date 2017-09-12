
import configs.syntax._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.Future
import scala.io.StdIn
import scala.language.postfixOps
import scala.sys.process.{ProcessIO, _}
import scala.util.{Failure, Success}

object ReactiveKafkaSymphonyProducer extends App {

  import akka.stream.scaladsl.{Framing, _}

  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val config = ConfigFactory.load()
  val bootstrapServers = config.getOrElse("bootstrapServers", "").toOption.fold("")(identity(_))

  println(s"Using bootstrap servers: ${bootstrapServers}")

  val keyfile = if (args.size < 1) Failure(new Exception) else Success(args(0))
  val local = if (args.size < 2) None else Some(args(1))

  val producerSettings = ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
    .withBootstrapServers(bootstrapServers)

  val marcFlow: Flow[ByteString, ByteString, NotUsed] = Framing.delimiter(ByteString(29.asInstanceOf[Char]),
    maximumFrameLength = 10000, allowTruncation = true)

  val recordFlow = Flow[ByteString].mapAsyncUnordered(16) { elem =>
    Future {
      ProducerMessage.Message(new ProducerRecord[String, Array[Byte]]( "marc21", null, (elem ++ ByteString(29.asInstanceOf[Char])).toArray), null)
    }
  }

  val processIO = new ProcessIO(
    (inOut: java.io.OutputStream) => {},

    (outIn: java.io.InputStream) => {

      val source = StreamConverters.fromInputStream(() => outIn)

      source.async.via(marcFlow).async.via(recordFlow).via(Producer.flow(producerSettings)).map { result =>
        val record = result.message.record
        println(s"Posted message ${record.value} to kafka ${record.topic} topic")
        result
      }.runWith(Sink.ignore)
    },

    (errIn: java.io.InputStream) => {} /*err*/,

    true
  )

  keyfile.map { file =>
    local match{
      case None => {
        (s"cat ${file}").run(processIO)
      }
      case Some(e) => {
        if (e == "symphony")
          (s"cat ${file}" #| "/s/sirsi/Unicorn/Bin/catalogdump -om -kc -h 2>/dev/null").run(processIO)
        else
          println("Any second argument will do, but usage should really be: ReactiveKafkaSymphonyProducer [ckey_file] symphony")
      }
    }
  }.recover {
    case _ => println("Missing file of catalog keys")
  }

  StdIn.readLine()
  system.terminate()

}