import java.io.{StringReader, StringWriter}
import javax.xml.transform.stream.StreamSource

import net.sf.saxon.lib.StandardURIResolver
import net.sf.saxon.s9api.Processor
import org.apache.spark.{SparkConf, SparkContext}
import java.io.{File => JFile}
import java.net.URL

import com.typesafe.config.ConfigFactory
import configs.syntax._
import org.apache.commons.io.IOUtils

import scala.util.{Failure, Success, Try}



object MarcXMLtoBibFrame  {


  def main(args: Array[String]): Unit = {


    /*val conf       = new SparkConf().setAppName("worksheet").setMaster("local[*]").setExecutorEnv("spark.executor.memory", "12g")
  val sc         = new SparkContext(conf)*/
    val config = ConfigFactory.load()
    val dir = config.getOrElse("dataDir", "").toOption.fold("")(identity(_))
    val sc = new SparkContext()


    val XMLtoBibFrameErrors = sc.longAccumulator("XMLtoBibFrameErrors")
    val MarcXmlRecords = sc.wholeTextFiles(s"${dir}/marc_xml_1000").repartition(14)


    MarcXmlRecords.cache()


    val BibFrameRDFRecords = MarcXmlRecords.mapPartitions { s =>

      val proc        = new Processor(false)
      val compiler    = Try{proc.newXsltCompiler()}
      val withResolve = compiler.map{c => /*c.setURIResolver(new StandardURIResolver());*/ c}
      val xsltExec    = withResolve.map{ compwr =>
        val stylesheet = new URL(s"file:///${dir}/xsl/marc2bibframe2.xsl")
        val source     = new StreamSource(IOUtils.toInputStream(IOUtils.toString(stylesheet.openStream())))
        source.setSystemId(stylesheet.toString)
        compwr.compile(source)
      }
      val transformer = xsltExec.map(_.load30())

      xsltExec match {
        case Success(e) => {};
        case Failure(e) => { System.err.println(s"Problem with the xsltExec: ${e.getMessage}")}
      }
      transformer match {
        case Success(e) => {};
        case Failure(e) => {System.err.println(s"Problem with the transformer: ${e.getMessage}")}
      }

      s.flatMap { e =>
        XMLtoBibFrameErrors.add(1L)
        val source = new StreamSource(new StringReader(e._2))
        val writer = new StringWriter()
        val trans = transformer.map(trs => trs.applyTemplates(source, trs.newSerializer(writer)))
        trans match {
          case Success(e) => {
            writer.close(); Some(writer.toString)
          }
          case Failure(e) => {
            writer.close(); System.err.println(s"failure in XSLT Transformation: ${e.getMessage}"); None
          }
        }
      }
    }

    //Call a fake action for for the execution of the Job

    BibFrameRDFRecords.map(e => e).foreach { e => println(e) }

    println(s"\nThe num ${XMLtoBibFrameErrors}\n")

    Thread.sleep(600000)

    sc.stop()
  }


}
