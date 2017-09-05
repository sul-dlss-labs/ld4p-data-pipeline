import java.nio.file.{FileSystems, Path}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{Flow, Sink, Source}
import better.files.File
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}
import configs.syntax._


object ReactiveFolderReader extends App {

  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val config                = ConfigFactory.load()
  val dir                   = config.getOrElse("dataDir", "").toOption.fold("")(identity(_))
  val inDir                 = File(s"${dir}/casalini_mrc")


  val fs = FileSystems.getDefault

  val source: Source[Path, NotUsed] = Directory.ls(inDir.path/*fs.getPath(path)*/)

  //val flow = Flow[Path].mapAsync(16)(e => Future{File(e.toAbsolutePath).byteArray ; println(s"A File Named: ${e.getFileName} loaded"); e})

  val flow = Flow[Path].mapAsyncUnordered(32)(e => Future{File(e.toAbsolutePath).byteArray ; println(s"A File Named: ${e.getFileName} loaded"); e})

  //val flow = Flow[Path].map(e => {File(e.toAbsolutePath).byteArray ; println(s"A File Named: ${e.getFileName} loaded"); e})

  /*val changes = DirectoryChangesSource(fs.getPath(path), pollInterval = 1.second, maxBufferSize = 1000)
  changes.runForeach {
    case (path, change) => println("Path: " + path + ", Change: " + change)
  }*/

  val starttime = System.currentTimeMillis()

  //val done = source.async.via(flow).async.runWith(Sink.ignore)

  val done = source.via(flow).runWith(Sink.ignore)


  done.onComplete {

    case Success(e) => println(s"Task succeedIn: ${System.currentTimeMillis() - starttime}")
    case Failure(e) => println(s"Task failed in: ${System.currentTimeMillis() - starttime}")


  }


  StdIn.readLine()
  system.terminate()

}
