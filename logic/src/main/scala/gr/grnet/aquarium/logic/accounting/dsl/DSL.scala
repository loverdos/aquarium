package gr.grnet.aquarium.logic.accounting.dsl

import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.util.{Date}
import scala.collection.JavaConversions._
import org.slf4j.{LoggerFactory}

class DSL

object DSL {

  val logger = LoggerFactory.getLogger(classOf[DSL])

  def parse(input: InputStream) = {
    val yaml = new Yaml()
    val policy = yaml.load(input).asInstanceOf[java.util.Map[String,_]].get("creditpolicy")

    logger.debug("Policy parsing started")

    val resources = asScalaBuffer(policy.asInstanceOf[java.util.Map[String,_]]
                                        .get("resources")
                                        .asInstanceOf[java.util.List[String]]).toList
    logger.debug("Resources", resources.toString())

    val policies = asScalaBuffer(policy.asInstanceOf[java.util.Map[String,_]]
                                        .get("policies")
                                        .asInstanceOf[java.util.List[String]]).toList

    val dslpolicy = DSLCreditPolicy(List(), List(), List(), List())
  }
}

case class DSLCreditPolicy (
  var policies: List[DSLPolicy],
  var pricelists: List[DSLPriceList],
  var resources: List[DSLResource],
  var agreements: List[DSLAgreement]
)

case class DSLResource(
  var name: String
)

case class DSLAgreement (
  var name: String,
  var overrides: String,
  var policy : List[DSLPolicy],
  var pricelist : List[DSLPriceList]
)

case class DSLPolicy (
  var name: String,
  var overrides: String,
  var algorithms: Map[DSLResource, String],
  var effective: DSLTimeFrame
)

case class DSLPriceList (
  var name: String,
  var overrides: String,
  var prices: Map[DSLResource,  Float],
  var effective: DSLTimeFrame
)

case class DSLTimeFrame (
  var name: String,
  var start: Date,
  var end: Date,
  var effective: List[Pair[Date,Date]]
)