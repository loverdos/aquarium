/*
 * Copyright 2011 GRNET S.A. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 *   1. Redistributions of source code must retain the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer.
 *
 *   2. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY GRNET S.A. ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GRNET S.A OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and
 * documentation are those of the authors and should not be
 * interpreted as representing official policies, either expressed
 * or implied, of GRNET S.A.
 */

package gr.grnet.aquarium.logic.accounting.dsl

import scala.collection.JavaConversions._
import com.kenai.crontabparser.impl.CronTabParserBridge
import java.io.{InputStreamReader, InputStream}
import gr.grnet.aquarium.util.Loggable
import gr.grnet.aquarium.util.yaml._
import java.util.{Calendar, Date}

/**
 * A parser for the Aquarium accounting DSL.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait DSL extends Loggable {

    /** An empty policy */
   val emptyPolicy = DSLPolicy("", None, Map(), DSLTimeFrame(new Date(0), None, Option(List())))

   /** An empty pricelist */
   val emptyPriceList = DSLPriceList("", None, Map(), DSLTimeFrame(new Date(0), None, Option(List())))

   /** An empty agreement*/
   val emptyAgreement = DSLAgreement("", None, emptyPolicy, emptyPriceList)

  /**
   * Parse an InputStream containing an Aquarium DSL policy.
   */
  def parse(input: InputStream) : DSLCreditPolicy = {
    logger.debug("Policy parsing started")

    val document = YAMLHelpers.loadYAML(new InputStreamReader(input))
    val policy = document / (Vocabulary.creditpolicy)

    val resources = parseResources(policy./(Vocabulary.resources).asInstanceOf[YAMLListNode])
    logger.debug("Resources: %s".format(resources))

    val policies = parsePolicies(policy./(Vocabulary.policies).asInstanceOf[YAMLListNode],resources, List())
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

    DSLCreditPolicy(policies, pricelists, resources, agreements)
  }

  /** Parse top level resources declarations */
  private def parseResources(resources: YAMLListNode): List[DSLResource] = {
    if (resources.isEmpty)
      return List()
    resources.head match {
      case x: YAMLStringNode =>
        List(DSLResource(x.string)) ++ parseResources(resources.tail)
      case _ =>
        throw new DSLParseException("Resource not a string:%s".format(resources.head))
    }
  }

  /** Parse top level policy declarations */
  private def parsePolicies(policies: YAMLListNode,
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
          case None => throw new DSLParseException("Cannot find super policy %s".format(superName))
        }
      case YAMLEmptyNode => emptyPolicy
      case _ => throw new DSLParseException("Super policy name %s not a string".format())
    }

    val policy = constructPolicy(policies.head.asInstanceOf[YAMLMapNode],
      policyTmpl, resources)

    val tmpresults = results ++ List(policy)
    List(policy) ++ parsePolicies(policies.tail, resources, tmpresults)
  }

  /** Construct a policy object from a yaml node*/
  def constructPolicy(policy: YAMLMapNode,
                      policyTmpl: DSLPolicy,
                      resources: List[DSLResource]): DSLPolicy = {
    val name = policy / Vocabulary.name match {
      case x: YAMLStringNode => x.string
      case YAMLEmptyNode => throw new DSLParseException("Policy does not have a name")
    }

    val overr = policy / Vocabulary.overrides match {
      case x: YAMLStringNode => Some(policyTmpl)
      case YAMLEmptyNode => None
    }

    val algos = resources.map {
      r =>
        val algo = policy / r.name match {
          case x: YAMLStringNode => x.string
          case y: YAMLIntNode => y.int.toString
          case YAMLEmptyNode => policyTmpl.equals(emptyPolicy) match {
            case false => policyTmpl.algorithms.getOrElse(r,
              throw new DSLParseException(("Superpolicy does not specify an algorithm for resource:%s").format(r.name)))
            case true => throw new DSLParseException(("Cannot find calculation algorithm for resource %s in either " +
              "policy %s or a superpolicy").format(r.name, name))
          }
        }
        Map(r -> algo)
    }.foldLeft(Map[DSLResource, String]())((x,y) => x ++ y)

    val timeframe = policy / Vocabulary.effective match {
      case x: YAMLMapNode => parseTimeFrame(x)
      case YAMLEmptyNode => policyTmpl.equals(emptyPolicy) match {
        case false => policyTmpl.effective
        case true => throw new DSLParseException(("Cannot find effectivity period for policy %s ").format(name))
      }
    }

    DSLPolicy(name, overr, algos, timeframe)
  }

  /** Parse top level pricelist declarations */
  private def parsePriceLists(pricelists: YAMLListNode,
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
          case None => throw new DSLParseException("Cannot find super pricelist %s".format(superName))
        }
      case YAMLEmptyNode => emptyPriceList
      case _ => throw new DSLParseException("Super pricelist name %s not a string".format())
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
      case YAMLEmptyNode => throw new DSLParseException(
        "Policy does not have a name")
    }

    val overr = pl / Vocabulary.overrides match {
      case x: YAMLStringNode => Some(tmpl)
      case YAMLEmptyNode => None
    }

    val prices = resources.map {
      r =>
        val price = pl / r.name match {
          case y: YAMLIntNode => y.int.toFloat
          case z: YAMLDoubleNode => z.double.toFloat
          case a: YAMLStringNode => a.string.toFloat
          case YAMLEmptyNode => tmpl.equals(emptyPolicy) match {
            case false => tmpl.prices.getOrElse(r,
              throw new DSLParseException(("Superpolicy does not specify a price for resource:%s").format(r.name)))
            case true => throw new DSLParseException(("Cannot find price for resource %s in either pricelist %s or " +
              "its super pricelist").format(r.name, name))
          }
        }
        Map(r -> price)
    }.foldLeft(Map[DSLResource, Float]())((x, y) => x ++ y)

    val timeframe = pl / Vocabulary.effective match {
      case x: YAMLMapNode => parseTimeFrame(x)
      case YAMLEmptyNode => tmpl.equals(emptyPolicy) match {
        case false => tmpl.effective
        case true => throw new DSLParseException(("Cannot find effectivity period for pricelist %s ").format(name))
      }
    }
    DSLPriceList(name, overr, prices, timeframe)
  }

  /** Parse top level agreements */
  private def parseAgreements(agreements: YAMLListNode,
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
           case None => throw new DSLParseException("Cannot find super agreement %s".format(superName))
         }
       case YAMLEmptyNode => emptyAgreement
       case _ => throw new DSLParseException("Super agreement name %s not a string".format(superName))
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
      case YAMLEmptyNode => throw new DSLParseException("Agreement does not have a name")
    }

    val policy = agr / Vocabulary.policy match {
      case x: YAMLStringNode => policies.find(p => p.name.equals(x.string)) match {
        case Some(y) => y
        case None => throw new DSLParseException(("Cannot find policy named %s").format(x))
      }
      case y: YAMLMapNode => tmpl.equals(emptyAgreement) match {
        case true => throw new DSLParseException(("Incomplete policy definition for agreement %s").format(name))
        case false =>
          y.map += ("name" -> YAMLStringNode("/","%s-policy".format(name)))
          constructPolicy(y, tmpl.policy, resources)
      }
      case YAMLEmptyNode => tmpl.equals(emptyAgreement) match {
        case true => throw new DSLParseException(("No policy for agreement %s").format(name))
        case false => tmpl.policy
      }
    }

    val pricelist = agr / Vocabulary.pricelist match {
      case x: YAMLStringNode => pricelists.find(p => p.name.equals(x.string)) match {
        case Some(y) => y
        case None => throw new DSLParseException(("Cannot find pricelist named %s").format(x))
      }
      case y: YAMLMapNode => tmpl.equals(emptyAgreement) match {
        case true => throw new DSLParseException(("Incomplete pricelist definition for agreement %s").format(name))
        case false =>
          y.map += ("name" -> YAMLStringNode("/","%s-pricelist".format(name)))
          constructPriceList(y, tmpl.pricelist, resources)
      }
      case YAMLEmptyNode => tmpl.equals(emptyAgreement) match {
        case true => throw new DSLParseException(("No policy for agreement %s").format(name))
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
      case _ => throw new DSLParseException("No %s field for timeframe %s".format(Vocabulary.from, timeframe))
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

  /** Parse a resource frame repeat block */
  def parseTimeFrameRepeat(tmr: YAMLListNode): List[DSLTimeFrameRepeat] = {

    if (tmr.isEmpty)
      return List()

    List(DSLTimeFrameRepeat(
      findInMap(tmr.head.asInstanceOf[YAMLMapNode], Vocabulary.start),
      findInMap(tmr.head.asInstanceOf[YAMLMapNode], Vocabulary.end)
    )) ++ parseTimeFrameRepeat(tmr.tail)
  }

  /** Parse a resource frame entry (start, end tags) */
  private def findInMap(repeat: YAMLMapNode,
                        tag: String) : List[DSLTimeSpec] = {
    repeat / tag match {
      case x: YAMLStringNode => parseCronString(x.string)
      case YAMLEmptyNode => throw new DSLParseException("No %s field for repeat entry %s".format(tag, repeat))
    }
  }

  /** 
   * Wraps the [[http://kenai.com/projects/crontab-parser/pages/Home crontabparser]]
   * library to parse crontab-like strings. The input format differs from the
   * [[http://en.wikipedia.org/wiki/Cron default cron format]] in the following ways:
   *
   *  - Only 5 field resource specs are allowed
   *  - Multiple values per field (e.g. Mon,Wed,Fri) are not allowed. Ranges
   *    (e.g. Mon-Fri) are however allowed.
   */
  def parseCronString(input: String): List[DSLTimeSpec] = {

    if (input.split(" ").length != 5)
      throw new DSLParseException("Only five-field cron strings allowed: " + input)

    if (input.contains(','))
      throw new DSLParseException("Multiple values per field are not allowed: " + input)

    val cron = try {
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

    splitMultiVals(cron.get(0).toString).map(
      a => splitMultiVals(cron.get(1).toString).map(
        b => splitMultiVals(cron.get(2).toString).map(
          c => splitMultiVals(cron.get(3).toString).map(
            d => splitMultiVals(cron.get(4).toString).map(
              e => DSLTimeSpec(a, b, c, d, e)
            )
          ).flatten
        ).flatten
      ).flatten
    ).flatten.toList
  }
}

case class DSLCreditPolicy (
  policies: List[DSLPolicy],
  pricelists: List[DSLPriceList],
  resources: List[DSLResource],
  agreements: List[DSLAgreement]
) {
  /** Find a resource by name */
  def findResource(name: String): Option[DSLResource] = {
    resources.find(a => a.name.equals(name))
  }

  /** Find a pricelist by name */
  def findPriceList(name: String): Option[DSLPriceList] = {
    pricelists.find(a => a.name.equals(name))
  }

  /** Find a pricelist by name */
  def findPolicy(name: String): Option[DSLPolicy] = {
    policies.find(a => a.name.equals(name))
  }

  /** Find an agreement by name */
  def findAgreement(name: String): Option[DSLAgreement] = {
    agreements.find(a => a.name.equals(name))
  }
}

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
  overrides: Option[DSLPolicy],
  algorithms: Map[DSLResource, String],
  effective: DSLTimeFrame
)

case class DSLPriceList (
  name: String,
  overrides: Option[DSLPriceList],
  prices: Map[DSLResource,  Float],
  effective: DSLTimeFrame
)

case class DSLTimeFrame (
  from: Date,
  to: Option[Date],
  repeat: Option[List[DSLTimeFrameRepeat]]
)

case class DSLTimeFrameRepeat (
  start: List[DSLTimeSpec],
  end: List[DSLTimeSpec]
)

case class DSLTimeSpec(
  min: Int,
  hour: Int,
  dom: Int,
  mon: Int,
  dow: Int
){
  //Preconditions to force correct values on object creation
  assert(-1 <= min && 60 > min)
  assert(-1 <= hour && 24 > hour)
  assert(-1 <= dom && 31 > dom && dom != 0)
  assert(-1 <= mon && 12 > mon && mon != 0)
  assert(-1 <= dow && 7 > dow)

  /** Day of week conversions to stay compatible with [[java.util.Calendar]] */
  def getCalendarDow(): Int = dow match {
    case 0 => Calendar.SUNDAY
    case 1 => Calendar.MONDAY
    case 2 => Calendar.TUESDAY
    case 3 => Calendar.WEDNESDAY
    case 4 => Calendar.THURSDAY
    case 5 => Calendar.FRIDAY
    case 6 => Calendar.SATURDAY
    case 7 => Calendar.SUNDAY
  }

  /** Month conversions to stay compatible with [[java.util.Calendar]] */
  def getCalendarMonth(): Int = dow match {
    case 1 => Calendar.JANUARY
    case 2 => Calendar.FEBRUARY
    case 3 => Calendar.MARCH
    case 4 => Calendar.APRIL
    case 5 => Calendar.MAY
    case 6 => Calendar.JUNE
    case 7 => Calendar.JULY
    case 8 => Calendar.AUGUST
    case 9 => Calendar.SEPTEMBER
    case 10 => Calendar.OCTOBER
    case 11 => Calendar.NOVEMBER
    case 12 => Calendar.DECEMBER
  }
}

/** Exception thrown when a parsing error occurs*/
class DSLParseException(msg: String) extends Exception(msg)
