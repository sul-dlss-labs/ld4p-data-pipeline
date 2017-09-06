import java.nio.file.{FileSystems, Path}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{Flow, Sink, Source}
import better.files.File
import org.marc4j.marc.Record
import akka.stream.scaladsl.Source._
import com.typesafe.config.ConfigFactory
import configs.syntax._

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

object ReactiveFolderCopier extends App {

  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val MAX_ALLOWED_FILES     = Int.MaxValue
  val config                = ConfigFactory.load()
  val dir                   = config.getOrElse("dataDir", "").toOption.fold("")(identity(_))
  val inDir                 = File(s"${dir}/Casalini_mrc")
  val outDir                = File(s"${dir}/Casalini_mrc_${MAX_ALLOWED_FILES}")



  val tryDir = Try {
    if (outDir.exists) {
      outDir.delete();
      outDir.createDirectory()
    } else
      outDir.createDirectory()
  }

  val source: Source[Path, NotUsed] = Directory.ls(inDir.path)

  val readflow  = Flow[Path].mapAsyncUnordered(16){ e => Future{ (e.getFileName.toString, File(e.toAbsolutePath).byteArray) } }
  val writeflow = Flow[(String, Array[Byte])].mapAsyncUnordered(16) { copy =>
    Future {
     tryDir.flatMap{ dir => Try {dir.createChild(copy._1)}}.foreach { file =>
       file.writeByteArray(copy._2)
     }
    }
  }


  /*val readflow  = Flow[Path].map{e => (e.getFileName.toString, File(e.toAbsolutePath).byteArray)}
  val writeflow = Flow[(String, Array[Byte])].map { copy =>
      tryDir.flatMap{ dir => Try {dir.createChild(copy._1)}}.foreach { file =>
        file.writeByteArray(copy._2)
      }
  }*/


  val starttime = System.currentTimeMillis()

  val done = source.async.via(readflow).async.via(writeflow).runWith(Sink.ignore)

  done.onComplete {
    case Success(e) => println(s"Task succeedIn: ${System.currentTimeMillis() - starttime}")
    case Failure(e) => println(s"Task failed in: ${System.currentTimeMillis() - starttime}")
  }

  StdIn.readLine()

  system.terminate()

}
