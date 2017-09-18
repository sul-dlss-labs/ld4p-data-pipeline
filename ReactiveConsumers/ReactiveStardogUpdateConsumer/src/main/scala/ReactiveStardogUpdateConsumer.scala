


import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URL
import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, SinkShape}
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, RunnableGraph, Sink}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.w3.banana.{RDFXMLWriterModule, TurtleReaderModule, _}
import org.w3.banana.jena.JenaModuleExtended

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import configs.syntax._


trait SparqlHttpModuleWithUpdate extends RDFModule {

  import java.net.URL

  implicit val sparqlHttp: SparqlEngine[Rdf, Try, URL] with SparqlUpdate[Rdf,Try,URL]

}

trait SPARQLExampleDependencies
  extends RDFModule
    with RDFOpsModule
    with SparqlOpsModule
    with SparqlHttpModuleWithUpdate
    with TurtleReaderModule
    with TurtleWriterModule
    with RDFXMLReaderModule


trait SparqlOpertions extends SPARQLExampleDependencies { self =>

  import ops._
  import sparqlOps._
  import sparqlHttp.sparqlEngineSyntax._
  import sparqlHttp.sparqlUpdateSyntax._


  def getURIForInstances(graphs: Try[PointedGraphs[Rdf]]) = {

    val tryworknodes = graphs.map(work => work.nodes)

    val uris = tryworknodes.map(_.map{ node =>
      ops.foldNode(node) (
        { case URI (uri) => uri },
        {case BNode(bnode) => ""},
        {case Literal(l) => ""}
      )
    })
    uris.map{_.foldLeft(List[String]()) ((b:List[String], s:String) =>b.+:(s))}
  }

  def getDeleteStatement(instances: Try[List[String]]): Try[List[String]] = {
    instances.map { list =>
        list.map { e =>
          s"""  ?s ?p0  <${e}> .
              <${e}> ?p1 ?o
           """.stripMargin
        }
      }
  }


  def executeUpdateQuery(data: Seq[String]): Unit = {
    println(s"got ${data.size} record to send")

    //Writing the update
    val to = new ByteArrayOutputStream
    data.foreach { elt =>
      val graph = rdfXMLReader.read(new ByteArrayInputStream(elt.getBytes(StandardCharsets.UTF_8)), "")
      graph match {
        case Success(e) => () //println("The read was a success apparently")
        case Failure(e) => println(s"Could not read the RDF/XML received: ${e}")
      }

      //========== Not Good
      val workuri       = getURIForInstances(graph.map{ g => g.getAllInstancesOf(URI("http://id.loc.gov/ontologies/bibframe/Work"))})

      val instanceURI   = getURIForInstances(graph.map{ g => g.getAllInstancesOf(URI("http://id.loc.gov/ontologies/bibframe/Instance"))})


      getDeleteStatement(workuri).foreach(_.foreach(println(_)))
      getDeleteStatement(instanceURI).foreach(_.foreach(println(_)))


      graph.foreach(turtleWriter.write(_, to, ""))
    }
    val endpoint = new URL("http://localhost:5820/CasaliniDB/update")
    val query    = parseUpdate(s"""INSERT DATA {${to.toString}}""".stripMargin).get
    val res      = endpoint.executeUpdate(query)
    res match {
      case Success(e) => ()
      case Failure(e) => println(s"It failed with: ${e.toString}")
    }
  }

}

object BalancerService {

  def balancer[In, Out](worker: Flow[In, Out, Any], workerCount: Int) = {

    import GraphDSL.Implicits._

      Sink.fromGraph(GraphDSL.create() { implicit b =>

      val balancer = b.add(Balance[In](workerCount, waitForAllDownstreams = false))

      for (_ <- 1 to workerCount) {
        // for each worker, add an edge from the balancer to the worker, then wire
        balancer ~> worker.async ~> Sink.ignore
      }

      SinkShape(balancer.in)
    })
  }

}


object ReactiveStardogUpdateConsumer extends App {

  println("Starting ReactiveStardogDumpConsumer ...")
  implicit val system       = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec           = system.dispatcher

  val config = ConfigFactory.load()
  val bootstrapServers = config.getOrElse("bootstrapServers", "").toOption.fold("")(identity(_))
  val stardogBatchSize = config.getOrElse("stardogBatchSize", "50").toOption.fold("50")(identity(_)).toInt

  println(s"Using bootstrap servers: ${bootstrapServers}")
  println(s"Using stardogBatchSize: ${stardogBatchSize}")

  object SparlOperationsWithJena extends SparqlOpertions with JenaModuleExtended

  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(bootstrapServers)
    .withGroupId("group1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  val subscription = Subscriptions.topics("bibframe")



  val kafkaSource = Consumer.plainSource(consumerSettings, subscription).map(e => e.value())

  val worker = Flow[String].groupedWithin(/*stardogBatchSize*/1, 20 second)
      .mapAsyncUnordered(1){ e =>
        Future {SparlOperationsWithJena.executeUpdateQuery(e)}
      }


  val g = BalancerService.balancer(worker, 1)


  kafkaSource.async.runWith(g)

/*  done.onComplete{
    case Success(e) => println("stream processed with success"); system.terminate()
    case Failure(e) => println("process ended with failure"); system.terminate()
  }*/
}
