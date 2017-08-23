object ResourceReader extends App {


  val stylesheet = "/xsl/marc2bibframe2.xsl"
  val rs = getClass.getResource(stylesheet)
  println(s"This is the file:${rs.getFile}")


}
