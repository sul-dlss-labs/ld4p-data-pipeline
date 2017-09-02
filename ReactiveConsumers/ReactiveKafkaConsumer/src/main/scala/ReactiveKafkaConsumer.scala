import java.io.ByteArrayInputStream

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.Future


/*object ReactiveKafkaConsumer extends App {

  println("Starting ReactiveKafkaConsumer ...")
  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("group1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val subscription = Subscriptions.topics("marc21")


  val done = Consumer.plainSource(consumerSettings, subscription)
    .mapAsync(1){
      e => Future {

        val reader = new MarcStreamReader(new ByteArrayInputStream(e.value()))

        if (reader.hasNext) {
          println(reader.next().toString)
        }
        else {
          println("Failed to read a record")
        }
      }
    }
    .runWith(Sink.ignore)
}*/
