package gr.grnet.aquarium.logic.credits.model

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed trait CreditDistributionWhen {
  def name: String
  def attributes: Map[String, String]
}

object CreditDistributionWhen {
  object Names {
    val CreditDistributionWhenManual = "CreditDistributionWhenManual"
    val CreditDistributionWhenPeriodic = "CreditDistributionWhenPeriodic"
    val CreditDistributionWhenOnCreditArrival = "CreditDistributionWhenOnCreditArrival"
  }
}

case object CreditDistributionWhenManual extends CreditDistributionWhen {
  def name = CreditDistributionWhen.Names.CreditDistributionWhenManual
  def attributes = Map()
}

case class CreditDistributionWhenPeriodic(cronPeriod: String) extends CreditDistributionWhen {
  def name = CreditDistributionWhen.Names.CreditDistributionWhenPeriodic
  def attributes = Map("cronPeriod" -> cronPeriod)
}

case object CreditDistributionWhenOnCreditArrival extends CreditDistributionWhen {
  def name = CreditDistributionWhen.Names.CreditDistributionWhenOnCreditArrival
  def attributes = Map()
}