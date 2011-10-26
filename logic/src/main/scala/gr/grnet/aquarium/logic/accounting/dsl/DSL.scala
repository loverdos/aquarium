package gr.grnet.aquarium.logic.accounting.dsl

import scala.collection.JavaConversions._
import com.kenai.crontabparser.impl.CronTabParserBridge
import java.io.{InputStreamReader, InputStream}
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.util.yaml._
import java.util.Date

/**
 * A parser and semantic analyser for credit DSL files
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
    val repeat = "repeat"
    val start = "start"
    val end = "end"
  }

  def parse(input: InputStream) : DSLCreditPolicy = {
    logger.debug("Policy parsing started")

    val document = YAMLHelpers.loadYAML(new InputStreamReader(input))
    val policy = document / (Vocabulary.creditpolicy)

    val resources = parseResources (policy./(Vocabulary.resources).asInstanceOf[YAMLListNode])
    logger.debug("Resources: %s".format(resources))

    val policies = parsePolicies(
      policy./(Vocabulary.policies).asInstanceOf[YAMLListNode],
      resources, List())

    logger.debug("Policies: %s".format(policies))

    DSLCreditPolicy(policies, List(), resources, List())
  }

  /** Parse top level resources declarations */
  def parseResources(resources: YAMLListNode): List[DSLResource] = {
    if (resources.isEmpty)
      return List()
    resources.head match {
      case x: YAMLStringNode =>
        List(DSLResource(x.string)) ++ parseResources(resources.tail)
      case _ =>
        throw new DSLParseException("Resource not string:%s".format(resources.head))
    }
  }

  /** Parse top level policy declarations */
  def parsePolicies(policies: YAMLListNode,
                    resources: List[DSLResource],
                    results: List[DSLPolicy]): List[DSLPolicy] = {

    policies.head match {
      case YAMLEmptyNode => return List()
      case _ =>
    }

    val policy = constructPolicy(policies.head.asInstanceOf[YAMLMapNode], resources)
    val supr = policies.head / Vocabulary.overrides

    val tmpresults = results ++ List(policy)
    List(policy) ++ parsePolicies(policies.tail, resources, tmpresults)
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

  /** Parse a timeframe declaration */
  def parseTimeFrame(timeframe: YAMLMapNode): DSLTimeFrame = {
    val from = timeframe / Vocabulary.from match {
      case x: YAMLIntNode => new Date(x.int)
      case _ => throw new DSLParseException(
        "No %s field for timeframe %s".format(Vocabulary.from, timeframe))
    }

    val to = timeframe / Vocabulary.to match {
      case x: YAMLIntNode => Some(new Date(x.int))
      case _ => None
    }

    val effective = timeframe / Vocabulary.repeat match {
      case x: YAMLListNode => Some(parseTimeFrameRepeat(x))
      case _ => None
    }

    DSLTimeFrame(from, to, effective)
  }

  /** Parse a time frame repeat block */
  def parseTimeFrameRepeat(tmr: YAMLListNode): List[DSLTimeFrameRepeat] = {

    if (tmr.isEmpty)
      return List()

    List(DSLTimeFrameRepeat(
      findInMap(tmr.head.asInstanceOf[YAMLMapNode], Vocabulary.start),
      findInMap(tmr.head.asInstanceOf[YAMLMapNode], Vocabulary.end)
    )) ++ parseTimeFrameRepeat(tmr.tail)
  }

  /** Parse a time frame entry (start, end tags) */
  def findInMap(repeat: YAMLMapNode, tag: String) : List[DSLCronSpec] = {
    repeat / tag match {
      case x: YAMLStringNode => parseCronString(x.string)
      case _ => throw new DSLParseException(
        "No %s field for repeat entry %s".format(tag, repeat))
    }
  }

  /** 
   * Wraps the crontabparser library to parse DSL formatted cron strings.
   */
  def parseCronString(input: String): List[DSLCronSpec] = {

    if (input.split(" ").length != 5)
      throw new DSLParseException(
        "Only five-field cron strings allowed: " + input)

    if (input.contains(','))
      throw new DSLParseException(
        "Multiple values per field are not allowed: " + input)

    val foo = try {
      asScalaBuffer(CronTabParserBridge.parse(input))
    } catch {
      case e => throw new DSLParseException(
        "Error parsing cron string: " + e.getMessage)
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

  /** Merge two policies, field by field */
  def mergePolicy(policy: DSLPolicy, onto: DSLPolicy): DSLPolicy = {
    DSLPolicy(onto.name, onto.overrides,
      mergeMaps(policy.algorithms, onto.algorithms),
      mergeTimeFrames(policy.effective, onto.effective))
  }

  /** Merge two timeframes */
  def mergeTimeFrames(timeframe: DSLTimeFrame,
                      onto: DSLTimeFrame) : DSLTimeFrame = {

    val to = timeframe.to match {
      case Some(x) => timeframe.to
      case None => onto.to
    }

    val eff = timeframe.repeat match {
      case None => onto.repeat
      case Some(x) if x == Nil => onto.repeat
      case _ => timeframe.repeat
    }

    DSLTimeFrame(timeframe.from, to, eff)
  }

  /** Merge input maps on a field by field basis. In case of duplicate keys
   *  values from the first map are prefered.
   */
  def mergeMaps[A, B](a: Map[A, B], b: Map[A, B]): Map[A, B] = {
    a ++ b.map{ case (k,v) => k -> (a.getOrElse(k,v)) }
  }

  /** Merge input maps on a field by field basis. In case of duplicate keys,
   *  the provided function is used to determine which value to keep in the
   *  merged map.
   */
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
  to: Option[Date],
  repeat: Option[List[DSLTimeFrameRepeat]]
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
