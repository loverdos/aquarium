package gr.grnet.aquarium.logic.accounting.dsl

import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import scala.collection.JavaConversions._
import org.slf4j.{LoggerFactory}
import java.util.Date

class DSL

object DSL {

  val logger = LoggerFactory.getLogger(classOf[DSL])

  def parse(input: InputStream) : DSLCreditPolicy = {
    val yaml = new Yaml()
    val policy = yaml.load(input).asInstanceOf[java.util.Map[String,_]].get("creditpolicy")

    logger.debug("Policy parsing started")

    val resources = asScalaBuffer(policy.asInstanceOf[java.util.Map[String,_]]
                                        .get("resources")
                                        .asInstanceOf[java.util.List[String]]).toList
    logger.debug("Resources", resources.toString())

    val policies = asScalaBuffer(policy.asInstanceOf[java.util.Map[String,_]]
                                        .get("policies")
                                        .asInstanceOf[java.util.List[_]])

    policies.map(
      p => mapAsScalaMap(p.asInstanceOf[java.util.Map[String,_]])
    )


    DSLCreditPolicy(List(), List(), List(), List())
  }

  def parsePolicies(policies: List[Map[String,_]],
                    resources: List[DSLResource],
                    results: List[DSLPolicy]): List[DSLPolicy] = {

    val supr = policies.head.getOrElse("extends", None)

    

    val result = DSLPolicy("", "", Map(), DSLTimeFrame("", new Date(0), new Date(1), List()))
    val tmpresults = results ++ List(result)
    List(result) ++ parsePolicies(policies.tail, resources, tmpresults)
  }

  /*def getPolicy(policy: DSLPolicy, policies: List[DSLPolicy]) : DSLPolicy = {
    policy.overrides match {
      case x: String => getPolicy(policy, policies)
      case None => policy
    }
  }*/

  def constructPolicy(policy: Map[String, String], resources: List[DSLResource]) : DSLPolicy = {
    val name = policy.getOrElse("name", None).asInstanceOf[String]
    val overr = policy.getOrElse("overrides", None).asInstanceOf[String]
    val algos = resources.map {
      r =>
        val algo = policy.get(r.name) match {
          case Some(x) => x
          case None => ""
        }
        Map(r -> algo)
    }

    DSLPolicy(name, overr,
              mergeMaps(algos)((v1: String, v2: String) => v1), null)
  }

  def mergePolicy(policy: DSLPolicy, onto: DSLPolicy) : DSLPolicy = {
    DSLPolicy(onto.name, onto.overrides,
              mergeMaps(policy.algorithms, onto.algorithms), null)
  }

  def mergeMaps[A, B](a: Map[A, B], b: Map[A, B]): Map[A, B] = {
    a ++ b.map{ case (k,v) => k -> (a.getOrElse(k,v)) }
  }

  def mergeMaps[A, B](ms: List[Map[A, B]])(f: (B, B) => B): Map[A, B] =
    (Map[A, B]() /: (for (m <- ms; kv <- m) yield kv)) {
      (a, kv) =>
        a + (if (a.contains(kv._1))
              kv._1 -> f(a(kv._1), kv._2)
            else kv)
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
