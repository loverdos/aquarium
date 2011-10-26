package gr.grnet.aquarium.logic.credits.model

/**
 * The credit type representation.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed trait CreditType {
  def name: String

  def value: Int

  def isOwn = false

  def isInherited = false

  def isUnKnown = false
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed abstract class CreditTypeSkeleton(_name: String, _value: Int) extends CreditType {
  def name = _name
  def value = _value
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case object OwnCreditType extends CreditTypeSkeleton(CreditType.Names.Own, CreditType.Values.Own) {
  override def isOwn = true
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case object InheritedCreditType extends CreditTypeSkeleton(CreditType.Names.Inherited, CreditType.Values.Inherited) {
  override def isInherited = true
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class UnknownCreditType(reason: Option[String]) extends CreditTypeSkeleton(CreditType.Names.Unknown, CreditType.Values.Unknown) {
  override def isUnKnown = true
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
object CreditType {

  object Values {
    val Own = 1
    val Inherited = 2
    val Unknown = 3
  }

  object Names {
    val Own = "Own"
    val Own_Lower = Own.toLowerCase
    val Inherited = "Inherited"
    val Inherited_Lower = Inherited.toLowerCase
    val Unknown = "Unknown"
    val Unknown_Lower = Unknown.toLowerCase
  }

  def fromValue(value: Int): CreditType = {
    value match {
      case Values.Own       => OwnCreditType
      case Values.Inherited => InheritedCreditType
      case Values.Unknown   => UnknownCreditType(None)
      case value            => UnknownCreditType(Some("Bad value %s".format(value)))
    }
  }

  def fromName(name: String): CreditType = {
    name match {
      case Names.Own       => OwnCreditType
      case Names.Inherited => InheritedCreditType
      case Names.Unknown   => UnknownCreditType(None)
      case name            => UnknownCreditType(Some("Bad name %s".format(name)))
    }
  }

  def fromNameIgnoreCase(name: String): CreditType = {
    name match {
      case null => UnknownCreditType(Some("null name"))
      case _    => name.toLowerCase match {
        case Names.Own_Lower       => OwnCreditType
        case Names.Inherited_Lower => InheritedCreditType
        case Names.Unknown_Lower   => UnknownCreditType(None)
        case name                  => UnknownCreditType(Some("Bad name %s".format(name)))
      }
    }
  }

}