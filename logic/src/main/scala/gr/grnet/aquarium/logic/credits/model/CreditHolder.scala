package gr.grnet.aquarium.logic.credits.model

/**
 * A credit holder is the entity to which credits can be assigned.
 *
 * Credit holders can be composite or not.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed trait CreditHolder {
  def prototypeId: String
  def name: String
  def isSingleDefinition: Boolean
  def isCompositeDefinition: Boolean
  def memberHolders: List[CreditHolder]
}

/**
 * These are fixed definitions, from which all other inherit.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
object CreditHolderProtos {
  object IDs {
    val SingleCreditHolderID    = "gr.grnet.aquarium.credits.model.holder.Single"
    val CompositeCreditHolderID = "gr.grnet.aquarium.credits.model.holder.Composite"
  }
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed abstract class CreditHolderSkeleton extends CreditHolder {
  def isSingleDefinition = false

  def isCompositeDefinition = false
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class SingleCreditHolder(
    name: String,
    prototypeId: String = CreditHolderProtos.IDs.SingleCreditHolderID)
  extends CreditHolderSkeleton {

  def memberHolders = Nil

  override def isSingleDefinition = true
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class CompositeCreditHolder(
    name: String,
    memberHolders: List[CreditHolder],
    prototypeId: String = CreditHolderProtos.IDs.CompositeCreditHolderID)
  extends CreditHolderSkeleton {

  override def isCompositeDefinition = true
}