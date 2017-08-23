import java.io.ByteArrayInputStream

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
//import org.apache.spark.util.SizeEstimator
//import org.marc4j.MarcStreamReader

val conf = new SparkConf().setAppName("workshit").setMaster("local[*]")

val sc = new SparkContext(conf)


val binaries = sc.binaryFiles("/Users/sul.maatari/IdeaProjects/Workshit/src/spike/scala/Casalini_mrc", 16)

val binariesPartitioned = binaries.repartition(16)

/*binariesPartitioned.flatMap{e =>
  val reader = new MarcStreamReader(new ByteArrayInputStream(e._2.toArray()))
  if (reader.hasNext)  Some((e._1, reader.next())) else None
}.toDebugString*/


sc.stop()