import java.io.ByteArrayInputStream

import AkkaStreamMarcReader.outDir
import better.files.File
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

import scala.collection.immutable.Queue
import scala.util.{Success, Try}
import java.io.{File => JFile}
import java.net.URL

new URL("file:///Users/sul.maatari/IdeaProjects/Workshit/src/spike/scala/xsl/marc2bibframe2.xsl")

val file = new JFile("file:///Users/sul.maatari/IdeaProjects/Workshit/src/spike/scala/xsl/marc2bibframe2.xsl")

file.toPath
file.toString
file.toURI.toASCIIString

List(1, 2, 3, 4) filter (_ % 2 == 0)


trait Transaction[Account] {

  def doTransaction(account: Account, amount: Int): Try[Account]

}

case class Account(no:Int)

class TransactionImpl extends Transaction[Account] {

  override def doTransaction(account: Account, amount: Int) = Success {
    account
  }

}

 "hello " + " again"


val x: Array[Byte] = Array()

//new ByteArrayInputStream()

Try { ()}


val dir = File("/Users/sul.maatari/IdeaProjects/Workshit/src/spike/scala/Casalini_mrc_full")

Queue(dir.list.toList:_*)

Queue(List(1,2,3):_*).dequeue._2.dequeue._2

/*val outDir = File("/Users/sul.maatari/IdeaProjects/Workshit/src/spike/scala/Casalini_mrc")

outDir.exists

outDir.createDirectory()

outDir.createChild("11887086", false)*/
