object Ld4pApp { // These would be extracted to be common to all our subprojects
  val metricRegistry = new com.codahale.metrics.MetricRegistry()
}
trait Instrumented extends nl.grons.metrics.scala.InstrumentedBuilder {
  val metricRegistry = Ld4pApp.metricRegistry
}
