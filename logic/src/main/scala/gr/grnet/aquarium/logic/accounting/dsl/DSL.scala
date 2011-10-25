package gr.grnet.aquarium.logic.accounting.dsl

import scala.collection.JavaConversions._
import java.util.Date
import com.kenai.crontabparser.impl.CronTabParserBridge
import java.io.{InputStreamReader, InputStream}
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.util.yaml.{YAMLStringNode, YAMLMapNode, YAMLListNode, YAMLHelpers}

/**
 * 
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
object DSL extends Loggable {

  object Vocabulary {
    val creditpolicy = "creditpolicy"
    val resources = "resources"
    val policies = "policies"
    val policy = "policy"
    val name = "name"
    val overrides = "overrides"
    val effective = "effective"
    val from = "from"
    val to = "to"
    val every = "every"
    val repeat = "repeat"
    val start = "start"
    val end = "end"
  }

  def parse(input: InputStream) : DSLCreditPolicy = {
    logger.debug("Policy parsing started")

    val document = YAMLHelpers.loadYAML(new InputStreamReader(input))
    val policy = document / (Vocabulary.creditpolicy)

    val resources = policy / Vocabulary.resources
    logger.debug("Resources %s".format(resources))

    val policies = policy / Vocabulary.policies

    DSLCreditPolicy(List(), List(), List(), List())
  }

  /** Parse top level policy declarations */
  def parsePolicies(policies: YAMLListNode,
                    resources: List[DSLResource],
                    results: List[DSLPolicy]): List[DSLPolicy] = {

    val supr = policies.head.mapValue.getOrElse("extends", None)

    val result = DSLPolicy("", Option(""), Map(), DSLTimeFrame(new Date(0), new Date(1), List()))
    val tmpresults = results ++ List(result)
    List(result) ++ parsePolicies(policies.tail, resources, tmpresults)
  }

  /** Construct a policy object from a yaml node*/
  def constructPolicy(policy: YAMLMapNode,
                      resources: List[DSLResource]): DSLPolicy = {
    val name = policy / Vocabulary.name match {
      case x: YAMLStringNode => x.string
      case _ => throw new DSLParseException("Policy does not have a name")
    }

    val overr = policy / Vocabulary.overrides match {
      case x: YAMLStringNode => Some(x.string)
      case _ => None
    }

    val algos = resources.map {
      r =>
        val algo = policy / r.name match {
          case x: YAMLStringNode => x.string
          case _ => ""
        }
        Map(r -> algo)
    }

    val timeframe = policy / Vocabulary.effective match {
      case x: YAMLMapNode => parseTimeFrame(x)
      case _ => throw new DSLParseException("No effectivity period for policy %s".format(name))
    }

    DSLPolicy(name, overr,
      mergeMaps(algos)((v1: String, v2: String) => v1), timeframe)
  }

  /** Merge two policies, field by field */
  def mergePolicy(policy: DSLPolicy, onto: DSLPolicy) : DSLPolicy = {
    DSLPolicy(onto.name, onto.overrides,
              mergeMaps(policy.algorithms, onto.algorithms), null)
  }

  /** Parse a timeframe declaration */
  def parseTimeFrame(timeframe: YAMLMapNode): DSLTimeFrame = {
    val from = timeframe / Vocabulary.from match {
      case x: YAMLStringNode => x.string
      case _ => throw new DSLParseException(
        "No %s field for timeframe %s".format(Vocabulary.from, timeframe))
    }

    val to = timeframe / Vocabulary.to match {
      case x: YAMLStringNode => new Date(x.string.toLong)
      case _ => new Date(Long.MaxValue)
    }

    val effective = timeframe / Vocabulary.repeat match {
      case x: YAMLListNode => parseTimeFrameRepeat(x)
      case _ => throw new DSLParseException(
        "No %s field for timeframe %s".format(Vocabulary.every, timeframe))
    }
    DSLTimeFrame(new Date(from), to, effective)
  }

  /***/
  def parseTimeFrameRepeat(tmr: YAMLListNode): List[DSLTimeFrameRepeat] = {

    

    List(DSLTimeFrameRepeat(List(DSLCronSpec(0,0,0,0,0)),
      List(DSLCronSpec(0,0,0,0,0))))
  }

  /** 
   * Wraps the crontabparser library to parse DSL formatted cron strings.
   */
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
  overrides: Option[String],
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
  every: List[DSLTimeFrameRepeat]
)

case class DSLTimeFrameRepeat (
  start: List[DSLCronSpec],
  end: List[DSLCronSpec]
)

case class DSLCronSpec(
  sec: Int,
  min: Int,
  hour: Int,
  dow: Int,
  mon: Int
)

class DSLParseException(msg: String) extends Exception(msg)