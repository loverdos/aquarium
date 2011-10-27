package gr.grnet.aquarium.logic.test
package test

import org.junit.Test
import org.junit.Assert._
import gr.grnet.aquarium.logic.credits.dsl.CreditsDSL
import gr.grnet.aquarium.logic.credits.model.CreditStructureClass
import java.io.{Reader, InputStreamReader, InputStream, StringReader}
import gr.grnet.aquarium.util.yaml.YAMLHelpers
import gr.grnet.aquarium.logic.credits.model.CreditStructureClass._
import gr.grnet.aquarium.util.Loggable

class CreditDSLTest extends Loggable {

  object Keys {
    val StructureClass = "structure_class"
    val Id = "id"
    val Name = "name"
    val Units = "units"
    val Grouping = "grouping"
  }

  def parseString(s: CharSequence): CreditStructureClass = {
    doParse(new StringReader(s.toString))
  }

  def parseStream(in: InputStream, encoding: String = "UTF-8", closeIn: Boolean = true): CreditStructureClass = {
    doParse(new InputStreamReader(in, encoding), closeIn)
  }

  // FIXME: implement
  private def doParse(r: Reader, closeReader: Boolean = true): CreditStructureClass = {
    val creditsDocument = YAMLHelpers.loadYAML(r, closeReader)

    val ystructureDef = creditsDocument / Keys.StructureClass
    val yId = ystructureDef / Keys.Id
    val yname  = ystructureDef / Keys.Name
    val yunits = ystructureDef / Keys.Units
    val ygrouping = ystructureDef / Keys.Grouping

    logger.debug("name = %s".format(yname))
    logger.debug("units = %s".format(yunits))
    logger.debug("grouping = %s".format(ygrouping))

    CreditStructureClass("", "", Nil)
  }
  @Test
  def testDSLLoad = {
    val structure = CreditsDSL.parseStream(getClass.getClassLoader.getResourceAsStream("credit-structure-greek-uni.yaml"))
    assertNotNull(structure)
  }
}