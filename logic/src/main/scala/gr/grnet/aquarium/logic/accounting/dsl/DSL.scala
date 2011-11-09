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

  private object Vocabulary {
    val creditpolicy = "creditpolicy"
    val resources = "resources"
    val policies = "policies"
    val policy = "policy"
    val pricelists = "pricelists"
    val pricelist = "pricelist"
    val agreements = "agreements"
    val agreement = "agreement"
    val name = "name"
    val overrides = "overrides"
    val effective = "effective"
    val from = "from"
    val to = "to"
    val repeat = "repeat"
    val start = "start"
    val end = "end"
  }

  private val emptyPolicy = DSLPolicy("", None, Map(),
    DSLTimeFrame(new Date(0), None, Option(List())))

  private val emptyPriceList = DSLPriceList("", None, Map(),
    DSLTimeFrame(new Date(0), None, Option(List())))

  private val emptyAgreement = DSLAgreement("", None, emptyPolicy,
    emptyPriceList)

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

    val pricelists = parsePriceLists(
      policy./(Vocabulary.pricelists).asInstanceOf[YAMLListNode],
      resources, List()
    )
    logger.debug("Pricelists: %s".format(pricelists))
    
    val agreements = parseAgreements(
      policy./(Vocabulary.agreements).asInstanceOf[YAMLListNode],
      policies, pricelists, resources, List()
    )
    logger.debug("Agreements: %s".format(agreements))

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

    val superName = policies.head / Vocabulary.overrides
    val policyTmpl = superName match {
      case y: YAMLStringNode =>
        results.find(p => p.name.equals(y.string)) match {
          case Some(x) => x
          case None => throw new DSLParseException(
            "Cannot find super policy %s".format(superName))
        }
      case YAMLEmptyNode => emptyPolicy
      case _ => throw new DSLParseException(
        "Super policy name %s not a string".format())
    }

    val policy = constructPolicy(policies.head.asInstanceOf[YAMLMapNode],
      policyTmpl, resources)

    val tmpresults = results ++ List(policy)
    List(policy) ++ parsePolicies(policies.tail, resources, tmpresults)
  }

  /** Construct a policy object from a yaml node*/
  private def constructPolicy(policy: YAMLMapNode,
                      policyTmpl: DSLPolicy,
                      resources: List[DSLResource]): DSLPolicy = {
    val name = policy / Vocabulary.name match {
      case x: YAMLStringNode => x.string
      case YAMLEmptyNode => throw new DSLParseException("Policy does not have a name")
    }

    val overr = policy / Vocabulary.overrides match {
      case x: YAMLStringNode => Some(x.string)
      case YAMLEmptyNode => None
    }

    val algos = resources.map {
      r =>
        val algo = policy / r.name match {
          case x: YAMLStringNode => x.string
          case y: YAMLIntNode => y.int.toString
          case YAMLEmptyNode => policyTmpl.equals(emptyPolicy) match {
            case false => policyTmpl.algorithms.getOrElse(r,
              throw new DSLParseException(("Severe! Superpolicy does not " +
                "specify an algorithm for resource:%s").format(r.name)))
            case true => throw new DSLParseException(("Cannot find " +
              "calculation algorithm for resource %s in either policy %s or a" +
              " superpolicy").format(r.name, name))
          }
        }
        Map(r -> algo)
    }.foldLeft(Map[DSLResource, String]())((x,y) => x ++ y)

    val timeframe = policy / Vocabulary.effective match {
      case x: YAMLMapNode => parseTimeFrame(x)
      case YAMLEmptyNode => policyTmpl.equals(emptyPolicy) match {
        case false => policyTmpl.effective
        case true => throw new DSLParseException(("Cannot find effectivity " +
          "period for policy %s ").format(name))
      }
    }

    DSLPolicy(name, overr, algos, timeframe)
  }

  /** Parse top level pricelist declarations */
  def parsePriceLists(pricelists: YAMLListNode,
                    resources: List[DSLResource],
                    results: List[DSLPriceList]): List[DSLPriceList] = {
    pricelists.head match {
      case YAMLEmptyNode => return List()
      case _ =>
    }

    val superName = pricelists.head / Vocabulary.overrides
    val tmpl = superName match {
      case y: YAMLStringNode =>
        results.find(p => p.name.equals(y.string)) match {
          case Some(x) => x
          case None => throw new DSLParseException(
            "Cannot find super pricelist %s".format(superName))
        }
      case YAMLEmptyNode => emptyPriceList
      case _ => throw new DSLParseException(
        "Super pricelist name %s not a string".format())
    }

    val pl = constructPriceList(pricelists.head.asInstanceOf[YAMLMapNode],
      tmpl, resources)

    val tmpresults = results ++ List(pl)
    List(pl) ++ parsePriceLists(pricelists.tail, resources, tmpresults)
  }

  /* Construct a pricelist from a YAML node and template, which may be
  * an empty pricelist or an inhereted pricelist definition.
  */
  def constructPriceList(pl: YAMLMapNode, tmpl: DSLPriceList,
                         resources: List[DSLResource]): DSLPriceList = {
    val name = pl / Vocabulary.name match {
      case x: YAMLStringNode => x.string
      case YAMLEmptyNode => throw new DSLParseException("Policy does not have a name")
    }

    val overr = pl / Vocabulary.overrides match {
      case x: YAMLStringNode => Some(x.string)
      case YAMLEmptyNode => None
    }

    val prices = resources.map {
      r =>
        val algo = pl / r.name match {
          case x: YAMLStringNode => x.string
          case y: YAMLIntNode => y.int.toString
          case z: YAMLDoubleNode => z.double.toString
          case YAMLEmptyNode => tmpl.equals(emptyPolicy) match {
            case false => tmpl.prices.getOrElse(r,
              throw new DSLParseException(("Severe! Superpolicy does not " +
                "specify a price for resource:%s").format(r.name)))
            case true => throw new DSLParseException(("Cannot find " +
              "price for resource %s in either pricelist %s or its" +
              " super pricelist").format(r.name, name))
          }
        }
        Map(r -> algo)
    }.foldLeft(Map[DSLResource, Any]())((x, y) => x ++ y)

    val timeframe = pl / Vocabulary.effective match {
      case x: YAMLMapNode => parseTimeFrame(x)
      case YAMLEmptyNode => tmpl.equals(emptyPolicy) match {
        case false => tmpl.effective
        case true => throw new DSLParseException(("Cannot find effectivity " +
          "period for pricelist %s ").format(name))
      }
    }
    DSLPriceList(name, overr, Map(), timeframe)
  }

  /** Parse top level agreements */
  def parseAgreements(agreements: YAMLListNode,
                      policies: List[DSLPolicy],
                      pricelists: List[DSLPriceList],
                      resources: List[DSLResource],
                      results: List[DSLAgreement]): List[DSLAgreement] = {
     agreements.head match {
       case YAMLEmptyNode => return List()
       case _ =>
     }

     val superName = agreements.head / Vocabulary.overrides
     val tmpl = superName match {
       case y: YAMLStringNode =>
         results.find(p => p.name.equals(y.string)) match {
           case Some(x) => x
           case None => throw new DSLParseException(
             "Cannot find super agreement %s".format(superName))
         }
       case YAMLEmptyNode => emptyAgreement
       case _ => throw new DSLParseException(
         "Super agreement name %s not a string".format(superName))
     }

     val agr = constructAgreement(agreements.head.asInstanceOf[YAMLMapNode],
       tmpl, policies, pricelists, resources)

     val tmpresults = results ++ List(agr)
     List(agr) ++ parseAgreements(agreements.tail, policies, pricelists,
       resources, tmpresults)
   }


  def constructAgreement(agr: YAMLMapNode,
                         tmpl: DSLAgreement,
                         policies: List[DSLPolicy],
                         pricelists: List[DSLPriceList],
                         resources: List[DSLResource]) : DSLAgreement = {
     val name = agr / Vocabulary.name match {
      case x: YAMLStringNode => x.string
      case YAMLEmptyNode => throw new DSLParseException("Agreement does not " +
        "have a name")
    }

    val policy = agr / Vocabulary.policy match {
      case x: YAMLStringNode => policies.find(p => p.name.equals(x.string)) match {
        case Some(y) => y
        case None => throw new DSLParseException(("Cannot find policy " +
          "named %s").format(x))
      }
      case y: YAMLMapNode => tmpl.equals(emptyAgreement) match {
        case true => throw new DSLParseException(("Incomplete policy " +
          "definition for agreement %s").format(name))
        case false =>
          y.map += ("name" -> YAMLStringNode("/","%s-policy".format(name)))
          constructPolicy(y, tmpl.policy, resources)
      }
      case YAMLEmptyNode => tmpl.equals(emptyAgreement) match {
        case true => throw new DSLParseException(("No policy " +
          "for agreement %s").format(name))
        case false => tmpl.policy
      }
    }

    val pricelist = agr / Vocabulary.pricelist match {
      case x: YAMLStringNode => pricelists.find(p => p.name.equals(x.string)) match {
        case Some(y) => y
        case None => throw new DSLParseException(("Cannot find pricelist " +
          "named %s").format(x))
      }
      case y: YAMLMapNode => tmpl.equals(emptyAgreement) match {
        case true => throw new DSLParseException(("Incomplete pricelist " +
          "definition for agreement %s").format(name))
        case false =>
          y.map += ("name" -> YAMLStringNode("/","%s-pricelist".format(name)))
          constructPriceList(y, tmpl.pricelist, resources)
      }
      case YAMLEmptyNode => tmpl.equals(emptyAgreement) match {
        case true => throw new DSLParseException(("No policy " +
          "for agreement %s").format(name))
        case false => tmpl.pricelist
      }
    }

    val overrides = tmpl.equals(emptyAgreement) match {
      case true => Some(tmpl)
      case false => None
    }

    DSLAgreement(name, overrides, policy, pricelist) 
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
      case YAMLEmptyNode => None
    }

    val effective = timeframe / Vocabulary.repeat match {
      case x: YAMLListNode => Some(parseTimeFrameRepeat(x))
      case YAMLEmptyNode => None
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
  private def findInMap(repeat: YAMLMapNode, tag: String) : List[DSLCronSpec] = {
    repeat / tag match {
      case x: YAMLStringNode => parseCronString(x.string)
      case YAMLEmptyNode => throw new DSLParseException(
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

    val cron = try {
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

    splitMultiVals(cron.get(0).toString).map(
      a => splitMultiVals(cron.get(1).toString).map(
        b => splitMultiVals(cron.get(2).toString).map(
          c => splitMultiVals(cron.get(3).toString).map(
            d => splitMultiVals(cron.get(4).toString).map(
              e => DSLCronSpec(a, b, c, d, e)
            )
          ).flatten
        ).flatten
      ).flatten
    ).flatten.toList
  }

  /** Merge two pricelists, field by field */
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
  private def mergeMaps[A, B](a: Map[A, B], b: Map[A, B]): Map[A, B] = {
    a ++ b.map{ case (k,v) => k -> (a.getOrElse(k,v)) }
  }

  /** Merge input maps on a field by field basis. In case of duplicate keys,
   *  the provided function is used to determine which value to keep in the
   *  merged map.
   */
  private def mergeMaps[A, B](ms: List[Map[A, B]])(f: (B, B) => B): Map[A, B] =
    (Map[A, B]() /: (for (m <- ms; kv <- m) yield kv)) {
      (a, kv) =>
        a + (if (a.contains(kv._1))
              kv._1 -> f(a(kv._1), kv._2)
            else kv)
    }

  /*Functions to search credit policy by name*/
  def findResource(policy: DSLCreditPolicy, name: String) : Option[DSLResource] = {
    policy.resources.find(a => a.name.equals(name))
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
  overrides: Option[DSLAgreement],
  policy : DSLPolicy,
  pricelist : DSLPriceList
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
  min: Int,
  hour: Int,
  dom: Int,
  mon: Int,
  dow: Int
)

class DSLParseException(msg: String) extends Exception(msg)
