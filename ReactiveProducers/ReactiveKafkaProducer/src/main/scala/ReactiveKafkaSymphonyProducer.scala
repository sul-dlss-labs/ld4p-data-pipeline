
import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import akka.util.ByteString

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.Future
import scala.io.StdIn
import scala.language.postfixOps
import scala.sys.process.{ProcessIO, _}

object ReactiveKafkaSymphonyProducer extends App {

  import akka.stream.scaladsl.{Framing, _}

  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val producerSettings = ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
    .withBootstrapServers("localhost:9092, 192.168.0.101:9092, 127.0.0.1:9092")

  val marcFlow: Flow[ByteString, ByteString, NotUsed] = Framing.delimiter(ByteString(29.asInstanceOf[Char]),
    maximumFrameLength = 10000, allowTruncation = true)

  val recordFlow = Flow[ByteString].mapAsyncUnordered(16) { elem =>
    Future {new ProducerRecord[String, ByteString]("marc21", null, elem ++ ByteString(29.asInstanceOf[Char]))}
  }

  val processIO = new ProcessIO(
    (inOut: java.io.OutputStream) => {},

    (outIn: java.io.InputStream) => {

      val source = StreamConverters.fromInputStream(() => outIn)

      source.async.via(marcFlow).async.via(recordFlow).runWith(Sink.foreachParallel(16) { e =>
        println(e.topic)
        println(e.value.decodeString("US-ASCII"))
      })
    },

    (errIn: java.io.InputStream) => {} /*err*/,

    true
  )

  ("cat /s/sirsi/casalini_all.ckey" #| "/s/sirsi/Unicorn/Bin/catalogdump -om -kc -h 2>/dev/null").run(processIO)

  StdIn.readLine()
  system.terminate()

}