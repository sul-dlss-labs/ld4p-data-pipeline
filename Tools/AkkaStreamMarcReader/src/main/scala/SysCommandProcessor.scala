
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Framing, Sink, Source, StreamConverters}
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString
import better.files.File
import com.typesafe.config.ConfigFactory
import org.marc4j.{MarcStreamReader, MarcStreamWriter}

import scala.concurrent.Future
import scala.io.StdIn
import scala.language.postfixOps
import scala.sys.process._
import akka.stream.scaladsl.Framing
import akka.stream.scaladsl._
import configs.syntax._

object SysCommandProcessor extends App {



  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val config                = ConfigFactory.load()
  val dir                   = config.getOrElse("dataDir", "").toOption.fold("")(identity(_))
  val inFile                = File(s"${dir}/Casalini_mrc_full/casalini0.mrc")


  val flow: Flow[ByteString, ByteString, NotUsed] = Framing.delimiter(ByteString(29.asInstanceOf[Char]), maximumFrameLength = 10000, allowTruncation = true)



  val processIO = new ProcessIO(
    (inOut: java.io.OutputStream) => {},

    (outIn: java.io.InputStream) => {

      val source = StreamConverters.fromInputStream(() => outIn)

      source.async.via(flow).runWith(Sink.foreachParallel(16) {
        e =>
          val in = new ByteArrayInputStream((e ++ ByteString(29.asInstanceOf[Char])).toArray)
          val reader = new MarcStreamReader(in)
          if (reader.hasNext()) {val record = reader.next(); println(record.toString)}
          else {println("Failure to read")}
      })
    },

    (errIn: java.io.InputStream) => {} /*err*/,

    true
  )


  s"cat ${inFile.path.toString}".run(processIO)

  StdIn.readLine()
  system.terminate()

}
