package gr.grnet.aquarium.logic.credits.model

/**
 * A credit unit is the entity to which credits can be assigned.
 *
 * Credit units can be composite or not.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class CreditUnit(name: String,  subUnits: List[CreditUnit]) {
  def isComposite = subUnits.size == 0
}