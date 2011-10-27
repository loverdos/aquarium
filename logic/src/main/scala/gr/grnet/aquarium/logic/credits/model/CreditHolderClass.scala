package gr.grnet.aquarium.logic.credits.model

/**
 * A credit holder definition that is used to instantiate credit holders.
 *
 * Notice the OOP parlance resemblance.

 * Credit holders can be composite or not.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed trait CreditHolderClass {
  def name: String
  def isSingleHolderClass: Boolean
  def isCompositeHolderClass: Boolean
  def members: List[CreditHolderClass]
  def creditDistributions: MembersCreditDistributionMap
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class SingleCreditHolderClass(name: String) extends CreditHolderClass {
  def members = Nil
  def creditDistributions = Map()
  def isSingleHolderClass = true
  def isCompositeHolderClass = false
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class CompositeCreditHolderClass(
    name: String,
    members: List[CreditHolderClass],
    creditDistributions: MembersCreditDistributionMap)
  extends CreditHolderClass {

  def isSingleHolderClass = true
  def isCompositeHolderClass = true
}