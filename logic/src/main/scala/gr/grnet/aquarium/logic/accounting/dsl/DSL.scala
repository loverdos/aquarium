package gr.grnet.aquarium.logic.accounting.dsl

import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import scala.collection.JavaConversions._
import org.slf4j.{LoggerFactory}
import java.util.Date
import com.kenai.crontabparser.impl.CronTabParserBridge
import collection.mutable.Buffer

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

    

    val result = DSLPolicy("", "", Map(), DSLTimeFrame(new Date(0), new Date(1), None))
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

  def parseTimeFrame(timeframe: Map[String,_]): DSLTimeFrame = {
    val from = timeframe.getOrElse("from", throw new DSLParseException("No from field for timeframe")).asInstanceOf[Long]

    val to = timeframe.get("to") match {
      case Some(x) => new Date(x.asInstanceOf[Long])
      case None => new Date(Long.MaxValue)
    }

    val effective = timeframe.get("repeat") match {
        case Some(x) => parseTimeFrameRepeat(x.asInstanceOf[Map[String,_]])
        case None => None
    }

    DSLTimeFrame(new Date(from), to, Option(List()))
  }

  def parseTimeFrameRepeat(tmr: Map[String,_]): List[DSLTimeFrameRepeat] = {
    List(DSLTimeFrameRepeat(DSLCronSpec(0,0,0,0,0), DSLCronSpec(0,0,0,0,0)))
  }

  def parseCronString(input: String): List[DSLCronSpec] = {

    if (input.split(" ").length != 5)
      throw new DSLParseException("Only five-field cron strings allowed: " + input)

    if (input.contains(','))
      throw new DSLParseException("Multiple values per field are not allowed: " + input)

    val foo = try {
      asScalaBuffer(CronTabParserBridge.parse(input))
    } catch {
      case e => throw new DSLParseException("Error parsing cron string: " + e.getMessage)
    }

    def splitMultiVals(input: String): Range = {
      if (input.equals("*"))
        return -1 until 0

      if (input.contains('-')) {
        val ints = input.split('-')
        ints(0).toInt until ints(1).toInt + 1
      } else {
        input.toInt until input.toInt + 1
      }
    }

    splitMultiVals(foo.get(0).toString).map(
      a => splitMultiVals(foo.get(1).toString).map(
        b => splitMultiVals(foo.get(2).toString).map(
          c => splitMultiVals(foo.get(3).toString).map(
            d => splitMultiVals(foo.get(4).toString).map(
              e => DSLCronSpec(a, b, c, d, e)
            )
          ).flatten
        ).flatten
      ).flatten
    ).flatten.toList
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
  policies: List[DSLPolicy],
  pricelists: List[DSLPriceList],
  resources: List[DSLResource],
  agreements: List[DSLAgreement]
)

case class DSLResource(
  name: String
)

case class DSLAgreement (
  name: String,
  overrides: Option[String],
  policy : List[DSLPolicy],
  pricelist : List[DSLPriceList]
)

case class DSLPolicy (
  name: String,
  overrides: String,
  algorithms: Map[DSLResource, String],
  effective: DSLTimeFrame
)

case class DSLPriceList (
  name: String,
  overrides: Option[String],
  prices: Map[DSLResource,  Float],
  effective: DSLTimeFrame
)

case class DSLTimeFrame (
  from: Date,
  end: Date,
  effective: Option[List[DSLTimeFrameRepeat]]
)

case class DSLTimeFrameRepeat (
  start: DSLCronSpec,
  end: DSLCronSpec
)

case class DSLCronSpec(
  sec: Int,
  min: Int,
  hour: Int,
  dow: Int,
  mon: Int
)

class DSLParseException(msg: String) extends Exception(msg)