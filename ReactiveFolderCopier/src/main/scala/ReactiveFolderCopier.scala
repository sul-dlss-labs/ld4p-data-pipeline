import java.nio.file.{FileSystems, Path}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{Flow, Sink, Source}
import better.files.File
import org.marc4j.marc.Record
import akka.stream.scaladsl.Source._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object ReactiveFolderCopier extends App {

  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val MAX_ALLOWED_FILES     = 1000
  val inDir                 = File("/Users/sul.maatari/IdeaProjects/Worksheet/src/spike/scala/Casalini_mrc")
  val outDir                = File(s"/Users/sul.maatari/IdeaProjects/Worksheet/src/spike/scala/Casalini_mrc_${MAX_ALLOWED_FILES}")
  //val RecordSource          = queue[Record](1000, akka.stream.OverflowStrategy.backpressure)



  val tryDir = Try {
    if (outDir.exists) {
      outDir.delete();
      outDir.createDirectory()
    } else
      outDir.createDirectory()
  }

  val path                          = inDir.path.toString
  val fs                            = FileSystems.getDefault
  val source: Source[Path, NotUsed] = Directory.ls(fs.getPath(path))



  val readflow  = Flow[Path].mapAsyncUnordered(8){ e => Future{ (e.getFileName.toString, File(e.toAbsolutePath).byteArray) } }


  val writeflow = Flow[(String, Array[Byte])].mapAsyncUnordered(8) { copy =>
    Future {
     tryDir.flatMap{ dir => Try {dir.createChild(copy._1)}}.foreach { file =>
       file.writeByteArray(copy._2)
     }
    }
  }


  val done = source.take(MAX_ALLOWED_FILES).async.via(readflow).async.via(writeflow).runWith(Sink.ignore)

  done.onComplete {
    case Success(e) => println("file copied with success"); system.terminate()
    case Failure(e) => println("process ended with failure"); system.terminate()
  }

}
