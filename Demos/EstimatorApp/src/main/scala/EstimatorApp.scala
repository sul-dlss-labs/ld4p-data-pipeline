import java.io.{ByteArrayOutputStream, StringReader, StringWriter, File => JFile}
import java.net.URL
import javax.xml.transform.stream.StreamSource

import net.sf.saxon.s9api.Processor
import org.apache.commons.io.IOUtils
import org.apache.spark.SparkContext
import org.marc4j.MarcStreamReader
import com.typesafe.config.ConfigFactory
import configs.syntax._

import scala.util.{Failure, Success, Try}


object marcToXML {

  def apply(record: org.marc4j.marc.Record): String = {
    val out = new ByteArrayOutputStream()
    val marcXmlWriter = new org.marc4j.MarcXmlWriter(out, true)
    marcXmlWriter.write(record)
    marcXmlWriter.close()
    out.toString("UTF-8")
  }

}


object EstimatorApp {

  def main(args: Array[String]): Unit = {

    /*val conf              = new SparkConf().setAppName("worksheet").setExecutorEnv("spark.executor.memory", "13g")
                            .setExecutorEnv("spark.driver.memory", "1g").setMaster("local[*]")
  val sc                  = new SparkContext(conf)*/
    val sc = new SparkContext()
    val marc4jErrors = sc.longAccumulator("mar4jErrors")
    val XMLtoBibFrameErrors = sc.longAccumulator("XMLtoBibFrameErrors")
    val config = ConfigFactory.load()
    val dir = config.getOrElse("dataDir", "").toOption.fold("")(identity(_))
    val byteArrayRecords = sc.binaryFiles(s"${dir}/Casalini_mrc_6000").repartition(14)


    val Marc4jRecords = byteArrayRecords.flatMap { e =>
      val reader = new MarcStreamReader(e._2.open())
      if (reader.hasNext) Some(reader.next()) else {
        marc4jErrors.add(1); None
      }
    }

    //Marc4jRecords.cache()

    val MarcXmlRecords = Marc4jRecords.map(e => marcToXML(e))

    val BibFrameRDFRecords = MarcXmlRecords.mapPartitions { s =>
      val proc = new Processor(false)
      val compiler = Try {proc.newXsltCompiler()}
      val withResolve = compiler.map { c => /*c.setURIResolver(new StandardURIResolver());*/ c }
      val xsltExec = withResolve.map { compwr =>
        val stylesheet = new URL(s"file:///${dir}/xsl/marc2bibframe2.xsl")
        val source = new StreamSource(IOUtils.toInputStream(IOUtils.toString(stylesheet.openStream())))
        source.setSystemId(stylesheet.toString)
        compwr.compile(source)
      }

      val transformer = xsltExec.map(_.load30())

      s.flatMap { e =>
        val source = new StreamSource(new StringReader(e))
        val writer = new StringWriter()
        val trans = transformer.map(trs => trs.applyTemplates(source, trs.newSerializer(writer)))
        trans match {
          case Success(e) => {
            System.err.println(s"\n=============\nRecord transformed with success: ${writer.toString}\n===============\n")
            writer.close()
            Some(writer.toString)
          }
          case Failure(e) => {
            XMLtoBibFrameErrors.add(1L)
            writer.close();
            System.err.println(s"\n=======\nfailure in XSLT Transformation:}\n======\n")
            e.printStackTrace()
            System.err.println("\n========\n")
            None
          }
        }
      }
    }


    //Call a fake action for for the execution of the Job
    BibFrameRDFRecords.foreach(e => ())

    println(s"The number of error is: ${XMLtoBibFrameErrors}")

    Thread.sleep(600000)

    sc.stop()
  }
}
