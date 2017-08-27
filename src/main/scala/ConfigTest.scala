
import com.typesafe.config.ConfigFactory
import configs.Configs
import configs.syntax._

object ConfigTest extends App {


  val config = ConfigFactory.load()

  val dir = config.getOrElse("dataDir", "").toOption.fold("")(identity(_))


}
