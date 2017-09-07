import scala.io.StdIn
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Random}

import com.codahale.metrics.ConsoleReporter
import java.util.concurrent.TimeUnit;

object OurApplication { // These would be extracted to be common to all our subprojects
  val metricRegistry = new com.codahale.metrics.MetricRegistry()
}
trait Instrumented extends nl.grons.metrics.scala.InstrumentedBuilder {
  val metricRegistry = OurApplication.metricRegistry
}

object SingleMetricExample extends Instrumented {
  val generator = new Random
  private[this] val loading = metrics.timer("execution")
  val reporter = ConsoleReporter.forRegistry(metricRegistry)
                                .convertRatesTo(TimeUnit.SECONDS)
                                .convertDurationsTo(TimeUnit.MILLISECONDS)
                                .build()

  // Sleeps 0, 1, 2 or 3 seconds randomly to simulate varying execution times
  def asyncDoIt(i: Int): Future[Int] = {
    val duration: Int = generator.nextInt(4)
    Future {
      println(s"  Sleeping $i for " + duration.toString)
      Thread.sleep(duration * 1000)
      duration
    }
  }

  def doIt(i: Int): Future[Int] = loading.timeFuture {
    asyncDoIt(i)
  }

  def main(args: Array[String]) {
    reporter.start(10, TimeUnit.SECONDS)
    (1 to 10)
      .map(elem => { println(s"before: $elem"); elem })
      .map(elem => {
        val f = doIt(elem)
        f onComplete {
          case Success(t) => println(s"    Elem $elem slept for " + t.toString)
          case Failure(t) => println("An error has occured: " + t.getMessage)
        }
        elem
      })

    println("\nOutputting metrics every 10 seconds, or press 'return' to exit.\n")
    StdIn.readLine() // Wait, to avoid closing the chain before the Futures complete
    reporter.stop
  }
}
