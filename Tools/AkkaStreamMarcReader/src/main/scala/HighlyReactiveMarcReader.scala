import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString
import better.files.File
import com.typesafe.config.ConfigFactory
import org.marc4j.{MarcStreamReader, MarcStreamWriter}

import scala.concurrent.Future
import scala.io.StdIn

/**
  *
  * A Reactive Stream based MarcReader
  *
  * The Program Demo Reading the Content of a file containing a dump of marc21 records unframed
  * It reads each of them and emit it downstream for further processing.
  * In this case printing to the console in parallel with 16 workers.
  * Furthermore the Reading stage is separate from the writing stage.
  *
  * It uses:
  *
  * - Akka-Stream FileIO that stream the content of a file as chunck of Bytes
  *
  * - Akka Stream Framing.delimiter to frame the Marc21 Records
  *
  * Note Marc4j is only used here for displaying the records, but it is not used to read the data.
  *
  * With a Reader as such when can integrate readying dump into a reactive stream that send data downstream
  * such as in KAFKA
  *
  */
object HighlyReactiveMarcReader  extends App {


  import akka.stream.scaladsl.Framing
  import akka.stream.scaladsl._
  import configs.syntax._

  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val config                = ConfigFactory.load()
  val dir                   = config.getOrElse("dataDir", "").toOption.fold("")(identity(_))
  val inFile                = File(s"${dir}/Casalini_mrc_full/casalini0.mrc")

  val binData               = FileIO.fromPath(inFile.path)


  val m21Stream = binData.async
    .via(Framing.delimiter(ByteString(29.asInstanceOf[Char]), maximumFrameLength = 10000, allowTruncation = true))
    .mapAsync(16) { e =>
      Future {
        val in = new ByteArrayInputStream((e ++ ByteString(29.asInstanceOf[Char])).toArray)
        val reader = new MarcStreamReader(in)
        if (reader.hasNext()) {val record = reader.next(); Some(record)}
        else {None}
      }
    }


  m21Stream.runForeach{
    case None => println("Failure to read")
    case Some(e) => println(e.toString)
  }

  StdIn.readLine()
  system.terminate()
}


