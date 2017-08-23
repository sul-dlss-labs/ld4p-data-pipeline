/*
import java.nio.charset.StandardCharsets
import java.nio.file.{FileSystems, Path}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.scaladsl.Source.queue
import better.files.File
import org.marc4j.marc.Record

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.{Path => HPath}
import org.apache.hadoop.io._


object Hwriter {

  import org.apache.hadoop.fs.FileSystem
  import org.apache.hadoop.io.SequenceFile
  import java.net.URI

  def writeSeqFile(fileName: String, content: Array[Byte]): Unit = {

    val uri: String = fileName
    val conf = new Configuration
    val fs: FileSystem = FileSystem.get(URI.create(uri), conf)
    val path = new HPath(uri)
    val key = new BytesWritable
    val value = new BytesWritable
    var writer: SequenceFile.Writer = null
    try {

      writer = SequenceFile.createWriter(fs, conf, path, classOf[BytesWritable], classOf[BytesWritable])

      key.set(new BytesWritable(fileName.getBytes(StandardCharsets.UTF_8)))
      value.set(new BytesWritable(content))

      writer.append(key, value)
    }
    finally IOUtils.closeStream(writer)


  }


}


object HadoopSequenfileConvertor extends App {



  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val MAX_ALLOWED_FILES     = 4000
  val inDir                 = File("/Users/sul.maatari/IdeaProjects/Workshit/src/spike/scala/Casalini_mrc")
  val outDir                = File(s"/Users/sul.maatari/IdeaProjects/Workshit/src/spike/scala/Casalini_mrc_hadoop")
  val RecordSource          = queue[Record](1000, akka.stream.OverflowStrategy.backpressure)



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
      /*tryDir.flatMap{ dir => Try { dir.createChild(copy._1)}}.foreach { file => file.writeByteArray(copy._2)}*/
      tryDir.flatMap(dir => Try{Hwriter.writeSeqFile(dir.uri.toASCIIString + copy._1, copy._2)})
    }
  }


  val done = source.take(MAX_ALLOWED_FILES).async.via(readflow).async.via(writeflow).runWith(Sink.ignore)

  done.onComplete {
    case Success(e) => println("file copied with success"); system.terminate()
    case Failure(e) => println("process ended with failure"); system.terminate()
  }
}
*/
