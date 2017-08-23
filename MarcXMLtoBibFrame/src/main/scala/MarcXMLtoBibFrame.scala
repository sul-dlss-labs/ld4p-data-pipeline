import java.io.{StringReader, StringWriter}
import javax.xml.transform.stream.StreamSource

import net.sf.saxon.lib.StandardURIResolver
import net.sf.saxon.s9api.Processor
import org.apache.spark.SparkContext
import java.io.{File => JFile}
import scala.util.{Failure, Success, Try}

object MarcXMLtoBibFrame extends App {


  /*val conf       = new SparkConf().setAppName("worksheet").setMaster("local[*]").setExecutorEnv("spark.executor.memory", "12g")
  val sc         = new SparkContext(conf)*/
  val stylesheet = getClass.getResource("/xsl/marc2bibframe2.xsl").getFile
  val sc         = new SparkContext()


  val XMLtoBibFrameErrors = sc.longAccumulator("XMLtoBibFrameErrors")
  val MarcXmlRecords      = sc.wholeTextFiles("/Users/sul.maatari/IdeaProjects/Workshit/src/spike/scala/marc_xml").repartition(14)

  println("Everything has been red")
  //MarcXmlRecords.cache()

  val BibFrameRDFRecords = MarcXmlRecords.mapPartitions {s =>

    val proc        = new Processor(false)
    val compiler    = Try{proc.newXsltCompiler()}
    val withResolve = compiler.map{c => c.setURIResolver(new StandardURIResolver()); c}
    val xsltExec    = withResolve.map{ a => a.compile(new StreamSource(new JFile(stylesheet))) }
    val transformer = xsltExec.map(_.load30())

    xsltExec match {case Success(e) => {}; case Failure(e) => {System.err.println(s"Problem with the xsltExec: ${e.getMessage}")}}
    transformer match {case Success(e) => {}; case Failure(e) => {System.err.println(s"Problem with the transformer: ${e.getMessage}")}}

    s.flatMap{ e =>
      val source = new StreamSource(new StringReader(e._2))
      val writer = new StringWriter()
      val trans = transformer.map(trs => trs.applyTemplates(source, trs.newSerializer(writer)))
      trans match {
        case Success(e) => {writer.close() ; Some(writer.toString)}
        case Failure(e) => {writer.close(); XMLtoBibFrameErrors.add(1); System.err.println(s"failure in XSLT Transformation: ${e.getMessage}"); None}
      }
    }
  }

  println("about to cache")

  //BibFrameRDFRecords.cache()


  //Call a fake action for for the execution of the Job
  BibFrameRDFRecords.foreach{e => ()}


  //StdIn.readLine()

  sc.stop()

}
