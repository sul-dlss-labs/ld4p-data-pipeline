import java.io.{ByteArrayInputStream, InputStream, StringReader, StringWriter, File => JFile}
import java.nio.file.{Path, Paths}

import MarcReaderActor.{BackPressure, ProcessFiles, ProcessRecords}
import better.files.File
import akka.actor.{Actor, ActorSystem, PoisonPill, Props, Status}
import akka.stream.scaladsl.Source._
import akka.stream.{ActorMaterializer, IOResult, QueueOfferResult}
import akka.stream.QueueOfferResult._
import akka.stream.scaladsl._
import akka.util.ByteString
import org.marc4j.{MarcReader, MarcStreamReader}
import org.marc4j.marc.Record

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import akka.pattern.pipe

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.io.StdIn
import org.marc4j.MarcStreamWriter
import org.marc4j.MarcWriter


object MarcReaderActor {

  case object ProcessRecords
  case object BackPressure
  case object ProcessFiles

  def props(file: File, sourceQueue: SourceQueueWithComplete[Record]): Props = Props(new MarcReaderActor(file, sourceQueue))

}

class MarcReaderActor(dir: File, sourceQueue: SourceQueueWithComplete[Record])  extends Actor {



  var reader     : MarcStreamReader = _
  var file       : File             = _
  var inStream   : InputStream      = _
  var lastRecord : Record           = _
  var recordnum  : Int              = 0
  var fileQueue                     = Queue(dir.list.toList:_*)
  implicit val ec                   = context.dispatcher


  override def receive: Receive = {

    case ProcessFiles => {

      fileQueue.nonEmpty match {

        case true => {
          val res    = fileQueue.dequeue
          file       = res._1
          inStream   = file.newInputStream
          reader = new MarcStreamReader(inStream)
          fileQueue  = res._2
          self ! ProcessRecords
        }

        case false => {
          println("We are done processing all the files")
          sourceQueue.complete()
          println(s"Processed ${recordnum} Records")
        }
      }
    }

    case ProcessRecords => {

      if (reader.hasNext()) {
        val record = reader.next()
        pipe(sourceQueue.offer(record)) to self
        lastRecord = record
        recordnum += 1
      }
      else {
        println(s"We are done processing the file: ${file.name} ! Onto the Next one");
        self ! ProcessFiles
      }
    }


    case f: QueueOfferResult => f match {

      case Enqueued => {self ! ProcessRecords}

      case Dropped => {println(s"opps a record was dropped: ${lastRecord.toString}"); self ! ProcessRecords}

      case QueueClosed => {println("The queue was closed, stopping"); self ! PoisonPill; sourceQueue.complete()/*context.system.terminate()*/}

      case QueueOfferResult.Failure(cause) => {
        println(s"A failure happened with Record: ${lastRecord.toString} ! The identified cause is: ${cause.getMessage}. NoneTheless Retrying in 10 milliseconds")
        context.system.scheduler.scheduleOnce(10 millisecond, self, BackPressure)
      }
    }

    case Status.Failure(e) => {println("Big Failure, Shutting down"); self ! PoisonPill; sourceQueue.complete()/*context.system.terminate()*/}


    case BackPressure => {println("Resuming after a back pressure"); pipe(sourceQueue.offer(lastRecord)) to self}
  }

}




object AkkaStreamMarcReader extends App {

  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher
  val inDir                 = File("/Users/sul.maatari/IdeaProjects/Workshit/src/spike/scala/Casalini_mrc_full_1")
  val outDir                = File("/Users/sul.maatari/IdeaProjects/Workshit/src/spike/scala/Casalini_mrc_1")
  val RecordSource          = queue[Record](1000, akka.stream.OverflowStrategy.backpressure)


  val tryDir = Try {
    if (outDir.exists) {
      outDir.delete();
      outDir.createDirectory()
    } else
      outDir.createDirectory()
  }

  val sourceQueue = RecordSource.async.to{Sink.foreachParallel(24){e =>

    val tryChild  = tryDir.map(dir => dir.createChild(e.getControlNumber).changeExtensionTo(".mrc"))

    tryChild.foreach{ child =>
       val writer = new MarcStreamWriter(child.newOutputStream)
       writer.write(e)
       writer.close()
    }
    tryChild match {
      case Failure(f) => println(s"File creation failed with error: ${f}")
      case _ =>
    }

  }}.run()

  val marcReader  = system.actorOf(MarcReaderActor.props(inDir, sourceQueue))

  marcReader ! MarcReaderActor.ProcessFiles

  val startTime = System.currentTimeMillis()

  sourceQueue.watchCompletion().onComplete{
    case Success(e) => println(s"Operation succeded in ${System.currentTimeMillis() - startTime}"); system.terminate()
    case Failure(e) => println(s"Operation failed in ${System.currentTimeMillis() - startTime} seconds with failure ${e.getMessage}"); system.terminate()
  }

  //StdIn.readLine()
  //marcReader ! PoisonPill
  //system.terminate()

}
