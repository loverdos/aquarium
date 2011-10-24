package gr.grnet.aquarium.logic.credits.dsl

import gr.grnet.aquarium.util.yaml.YAMLHelpers
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.logic.credits.model.{CreditStructureDef, CreditStructure}
import java.io.{StringReader, InputStreamReader, Reader, InputStream}

/**
 *
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
object CreditsDSL extends Loggable {
  object Keys {
    val StructureDef = "structure_def"
    val Id = "id"
    val Name = "name"
    val Units = "units"
    val Grouping = "grouping"
  }

  def parseString(s: CharSequence): CreditStructureDef = {
    doParse(new StringReader(s.toString))
  }

  def parseStream(in: InputStream, encoding: String = "UTF-8", closeIn: Boolean = true): CreditStructureDef = {
    doParse(new InputStreamReader(in, encoding), closeIn)
  }

  // FIXME: implement
  private def doParse(r: Reader, closeReader: Boolean = true): CreditStructureDef = {
    val creditsDocument = YAMLHelpers.loadYAML(r, closeReader)

    val ystructureDef = creditsDocument / Keys.StructureDef
    val yId = ystructureDef / Keys.Id
    val yname  = ystructureDef / Keys.Name
    val yunits = ystructureDef / Keys.Units
    val ygrouping = ystructureDef / Keys.Grouping

    logger.debug("name = %s".format(yname))
    logger.debug("units = %s".format(yunits))
    logger.debug("grouping = %s".format(ygrouping))

    CreditStructureDef("", "", Nil, None)
  }
}