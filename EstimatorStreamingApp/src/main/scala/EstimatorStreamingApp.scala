import java.io.{ByteArrayInputStream, ByteArrayOutputStream, StringReader, StringWriter}
import java.net.URL
import javax.xml.transform.stream.StreamSource

import com.typesafe.config.ConfigFactory
import net.sf.saxon.s9api.Processor
import org.apache.commons.io.IOUtils
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.marc4j.MarcStreamReader
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


object EstimatorStreamingApp  {

  def main(args: Array[String]): Unit = {


    //val conf   = new SparkConf().setAppName("EstimatorStreaming").setMaster("local[*]").set("spark.streaming.backpressure.enabled", "true")
    val conf = new SparkConf().set("spark.streaming.backpressure.enabled", "true")
    val ssc = new StreamingContext(conf, Seconds(2))


    val marc4jErrors = ssc.sparkContext.longAccumulator("mar4jErrors")
    val XMLtoBibFrameErrors = ssc.sparkContext.longAccumulator("XMLtoBibFrameErrors")

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092, 192.168.0.101:9092, Maatari-Stanford.local:9092, 127.0.0.1:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[ByteArrayDeserializer],
      "group.id" -> "use_a_separate_group_id_for_each_stream",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val topics = Array("marc21")
    val stream = KafkaUtils.createDirectStream[String, Array[Byte]](
      ssc,
      PreferConsistent,
      Subscribe[String, Array[Byte]](topics, kafkaParams)
    )

    val Marc4jRecords = stream.flatMap { e =>

      val reader = new MarcStreamReader(new ByteArrayInputStream(e.value()))

      if (reader.hasNext) {
        val record = reader.next()
        Some(record)
      }
      else {
        println("\n====\nCould not read a Record\n====\n")
        marc4jErrors.add(1L)
        None
      }
    }

    val MarcXmlRecords = Marc4jRecords.map(e => marcToXML(e))

    val BibFrameRDFRecords = MarcXmlRecords.mapPartitions { s =>
      val config = ConfigFactory.load()
      val dir = config.getOrElse("dataDir", "").toOption.fold("")(identity(_))
      val proc = new Processor(false)
      val compiler = Try {
        proc.newXsltCompiler()
      }
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
            //System.err.println(s"\n=============\nRecord transformed with success: ${writer.toString}\n===============\n")
            writer.close()
            Some(writer.toString)
          }
          case Failure(e) => {
            XMLtoBibFrameErrors.add(1)
            writer.close();
            System.err.println(s"\n=======\nfailure in XSLT Transformation:}\n======\n")
            e.printStackTrace()
            System.err.println("\n========\n")
            None
          }
        }
      }
    }

    BibFrameRDFRecords.foreachRDD { rdd =>
      rdd.foreach(record => println(s"Processed record: \n======\n${record}\n======\n"))
    }

    ssc.start()

    ssc.awaitTermination()
  }
}
