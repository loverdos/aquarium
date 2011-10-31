package gr.grnet.aquarium.logic.credits.model

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed trait CreditDistributionHow {
  def name: String
  def attributes: Map[String, String]
}

object CreditDistributionHow {
  object Names {
    val CreditDistributionHowFixedAny = "CreditDistributionHowFixedAny"
    val CreditDistributionHowFixedEqual = "CreditDistributionHowFixedEqual"
    val CreditDistributionHowOnDemandUnlimited = "CreditDistributionHowOnDemandUnlimited"
    val CreditDistributionHowOnDemandMax = "CreditDistributionHowOnDemandMax"
    val CreditDistributionHowAlgorithmic = "CreditDistributionHowAlgorithmic"

  }
}

case object CreditDistributionHowFixedAny extends CreditDistributionHow {
  def name = CreditDistributionHow.Names.CreditDistributionHowFixedAny
  def attributes = Map()
}

case object CreditDistributionHowFixedEqual extends CreditDistributionHow {
  def name = CreditDistributionHow.Names.CreditDistributionHowFixedEqual
  def attributes = Map()
}

case object CreditDistributionHowOnDemandUnlimited extends CreditDistributionHow {
  def name = CreditDistributionHow.Names.CreditDistributionHowOnDemandUnlimited
  def attributes = Map()
}

case class CreditDistributionHowOnDemandMax(max: Long) extends CreditDistributionHow {
  def name = CreditDistributionHow.Names.CreditDistributionHowOnDemandMax
  def attributes = Map("max" -> max.toString)
}

case class CreditDistributionHowAlgorithmic(algorithm: String) extends CreditDistributionHow {
  def name = CreditDistributionHow.Names.CreditDistributionHowAlgorithmic
  def attributes = Map("algorithm" -> algorithm)
}