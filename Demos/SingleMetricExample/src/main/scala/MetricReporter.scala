import java.util.concurrent.TimeUnit
import com.codahale.metrics.ConsoleReporter

// This demonstrates a separate reporter for SingleMetricExample's metrics
object MetricReporter extends Instrumented {
  val reporter = ConsoleReporter.forRegistry(metricRegistry)
                                .convertRatesTo(TimeUnit.SECONDS)
                                .convertDurationsTo(TimeUnit.MILLISECONDS)
                                .build()
}
