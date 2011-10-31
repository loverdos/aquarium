package gr.grnet.aquarium.logic.credits.model

/**
 * Defines how credits have been obtained.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed trait CreditOrigin {
  def name: String

  def isOwn: Boolean = !isInherited
  def isInherited: Boolean = !isOwn
}

object CreditOrigin {
  object Names {
    val Own = "Own"
    val Inherited = "Inherited"
  }
}

case object OwnCreditOrigin extends CreditOrigin {
  override def isOwn = true

  def name = CreditOrigin.Names.Own
}

case class InheritedCreditOrigin(creditHolder: CreditHolder, creditDistributionPolicy: CreditDistributionPolicy) extends CreditOrigin {
  override def isInherited = true

  def name = CreditOrigin.Names.Own
}

