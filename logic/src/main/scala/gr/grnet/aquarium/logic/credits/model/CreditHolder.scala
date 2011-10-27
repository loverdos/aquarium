package gr.grnet.aquarium.logic.credits.model

/**
 * A credit holder is the entity to which credits can be assigned.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class CreditHolder(name: String, creditHolderClass: CreditHolderClass) {
  def isSingleHolderClass    = creditHolderClass.isSingleHolderClass
  def isCompositeHolderClass = creditHolderClass.isCompositeHolderClass
}