import javax.xml.transform.{TransformerException, URIResolver}

import org.apache.commons.logging.LogFactory


import java.io.IOException
import java.net.URL
import java.util
import javax.xml.transform.Source
import javax.xml.transform.TransformerException
import javax.xml.transform.URIResolver
import javax.xml.transform.stream.StreamSource
import org.apache.commons.io.IOUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

object S3URIResolver { // Logger
  private val log = LogFactory.getLog(classOf[S3URIResolver])
}


/**
  * URI Resolver that will be used by XLTProcessor when resolving xsl:import and
  * xsl:include statements. The purpose of this class is to create a local cache
  * of the stylesheets retrieved (presumably) from S3 so they don't need to be
  * re-retrieved from S3 on subsequent requests.
  *
  * @author Darin McBeath
  *
  */
class S3URIResolver extends URIResolver { // Stylesheet map cache

  private val stylesheetMap = new util.HashMap[String, String]
  private val log = LogFactory.getLog(classOf[S3URIResolver])


  @throws[TransformerException]
  def resolve(href: String, base: String): Source = {

    try {
      if (stylesheetMap.containsKey(href))
        new StreamSource(IOUtils.toInputStream(stylesheetMap.get(href)))
      else {
        val theUrl = new URL(href)
        stylesheetMap.put(href, IOUtils.toString(theUrl.openStream))
        new StreamSource(IOUtils.toInputStream(stylesheetMap.get(href)))
      }
    }
    catch {
      case e: IOException =>
        S3URIResolver.log.error("Problems resolving a stylesheet. URI:" + href + " " + e.getMessage, e)
        new TransformerException(e.getMessage);
        null
    }

  }
}

