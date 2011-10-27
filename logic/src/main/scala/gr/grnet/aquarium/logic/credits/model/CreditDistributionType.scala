package gr.grnet.aquarium.logic.credits.model

/**
 * The credit distribution type representation.
 *
 * This dictates how credits are distributed at lower level structure parts, for example
 * how a University distributes credits to its Departments.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed trait CreditDistributionType {
  def name: String
  def value: Int

  def isUnknown = false
  def isFixed = isFixedAny || isFixedEqual
  def isFixedAny = false
  def isFixedEqual = false
  def isOnDemandUnlimited = false
  def isOnDemandMax = false
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed abstract class CreditDistributionTypeSkeleton(_name: String,  _value: Int) extends CreditDistributionType {
  def name = _name
  def value = _value
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case object FixedAnyCreditDistributionType
  extends CreditDistributionTypeSkeleton(CreditDistributionType.Names.FixedAny, CreditDistributionType.Values.FixedEqual) {

  override def isFixedAny = true
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case object FixedEqualCreditDistributionType
  extends CreditDistributionTypeSkeleton(CreditDistributionType.Names.FixedAny, CreditDistributionType.Values.FixedEqual) {

  override def isFixedEqual = true
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case object OnDemandUnlimitedCreditDistributionType
  extends CreditDistributionTypeSkeleton(CreditDistributionType.Names.OnDemandUnlimited, CreditDistributionType.Values.OnDemandUnlimited) {

  override def isOnDemandUnlimited = true
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case object OnDemandMaxCreditDistributionType
  extends CreditDistributionTypeSkeleton(CreditDistributionType.Names.OnDemandMax, CreditDistributionType.Values.OnDemandMax) {

  override def isOnDemandMax = true
}

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class UnknownCreditDistributionType(reason: Option[String]) extends CreditDistributionTypeSkeleton(CreditDistributionType.Names.Unknown, CreditDistributionType.Values.Unknown) {
  override def isUnknown = true
}

object CreditDistributionType {
  object Values {
    /**
     * Credits are distributed (pushed) in fixed values and parts can be divided at will.
     */
    val FixedAny = 10

    /**
     * Credits are distributed (pushed) in fixed values and parts are divided equally.
     */
    val FixedEqual = 20

    /**
     * Credits are distributed (pulled) on demand.
     */
    val OnDemandUnlimited = 30

    /**
     * Credits are distributed (pulled) on demand but up to a maximum value.
     */
    val OnDemandMax = 40

    /**
     * Error indicator
     */
    val Unknown = -1
  }

  object Names {
    val FixedAny = "FixedAny"
    val FixedEqual = "FixedEqual"
    val OnDemandUnlimited = "OnDemandUnlimited"
    val OnDemandMax = "OnDemandMax"
    val Unknown = "Unknown"

    val FixedAny_Lower = FixedAny.toLowerCase
    val FixedEqual_Lower = FixedEqual.toLowerCase
    val OnDemandUnlimited_Lower = OnDemandUnlimited.toLowerCase
    val OnDemandMax_Lower = OnDemandMax.toLowerCase
    val Unknown_Lower = Unknown.toLowerCase
  }

  def fromValue(value: Int): CreditDistributionType = {
    value match {
      case Values.FixedAny          => FixedAnyCreditDistributionType
      case Values.FixedEqual        => FixedEqualCreditDistributionType
      case Values.OnDemandUnlimited => OnDemandUnlimitedCreditDistributionType
      case Values.OnDemandMax       => OnDemandMaxCreditDistributionType
      case Values.Unknown           => UnknownCreditDistributionType(None)
      case value                    => UnknownCreditDistributionType(Some("Bad value %s".format(value)))
    }
  }

  def fromName(name: String): CreditDistributionType = {
    name match {
      case Names.FixedAny          => FixedAnyCreditDistributionType
      case Names.FixedEqual        => FixedEqualCreditDistributionType
      case Names.OnDemandUnlimited => OnDemandUnlimitedCreditDistributionType
      case Names.OnDemandMax       => OnDemandMaxCreditDistributionType
      case Names.Unknown           => UnknownCreditDistributionType(None)
      case value                   => UnknownCreditDistributionType(Some("Bad value %s".format(value)))
    }
  }

  def fromNameIgnoreCase(name: String): CreditDistributionType = {
    name match {
      case null => UnknownCreditDistributionType(Some("null name"))
      case _    => name.toLowerCase match {
        case Names.FixedAny_Lower          => FixedAnyCreditDistributionType
        case Names.FixedEqual_Lower        => FixedEqualCreditDistributionType
        case Names.OnDemandUnlimited_Lower => OnDemandUnlimitedCreditDistributionType
        case Names.OnDemandMax_Lower       => OnDemandMaxCreditDistributionType
        case Names.Unknown_Lower           => UnknownCreditDistributionType(None)
        case value                         => UnknownCreditDistributionType(Some("Bad value %s".format(value)))
      }
    }
  }
}
