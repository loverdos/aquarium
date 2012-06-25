/*
 * Copyright 2011-2012 GRNET S.A. All rights reserved.
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
import gr.grnet.aquarium.util.yaml._
import java.util.Date
import java.io.{ByteArrayInputStream, InputStreamReader, InputStream}

/**
 * A parser for the Aquarium accounting DSL.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait DSL {

  /**
   * Parse an InputStream containing an Aquarium DSL.
   */
  @throws(classOf[DSLParseException])
  def parse(input: InputStream) : DSLPolicy = {

    val document = YAMLHelpers.loadYAML(new InputStreamReader(input))
    val policy = document / (Vocabulary.aquariumpolicy)

    val resources = parseResources(policy./(Vocabulary.resources).asInstanceOf[YAMLListNode])

    val policies = parseAlgorithms(policy./(Vocabulary.algorithms).asInstanceOf[YAMLListNode],resources, List())

    val pricelists = parsePriceLists(
      policy./(Vocabulary.pricelists).asInstanceOf[YAMLListNode],
      resources, List()
    )

    val creditplans = parseCreditPlans(
      policy./(Vocabulary.creditplans).asInstanceOf[YAMLListNode], List()
    )

    val agreements = parseAgreements(
      policy./(Vocabulary.agreements).asInstanceOf[YAMLListNode],
      policies, pricelists, resources, creditplans, List()
    )

    DSLPolicy(policies, pricelists, resources, creditplans, agreements)
  }

  /**
   * Parse an InputStream containing an Aquarium DSL.
   */
  @throws(classOf[DSLParseException])
  def parse(yaml: String): DSLPolicy = parse(new ByteArrayInputStream(yaml.getBytes))

  /** Parse resource declarations */
  private def parseResources(resources: YAMLListNode): List[DSLResource] = {
    if (resources.isEmpty)
      return List()
    resources.head match {
      case x: YAMLMapNode => constructResource(x) :: parseResources(resources.tail)
      case _ => throw new DSLParseException("Resource format unknown")
    }
  }

  def constructResource(resource: YAMLMapNode): DSLResource = {
    val name = resource / Vocabulary.name match {
      case x: YAMLStringNode => x.string
      case YAMLEmptyNode => throw new DSLParseException("Resource does not have a name")
    }

    val unit = resource / Vocabulary.unit match {
      case x: YAMLStringNode => x.string
      case YAMLEmptyNode => throw new DSLParseException("Resource %s does specify a unit".format(name))
    }

    val complex = resource / Vocabulary.complex match {
      case x: YAMLBooleanNode => x.bool
      case _ => throw new DSLParseException("Resource %s does specify a complex value".format(name))
    }

    val costpolicy = resource / Vocabulary.costpolicy match {
      case x: YAMLStringNode => DSLCostPolicy(x.string)
      case _ => throw new DSLParseException("Resource %s does specify a cost policy".format(name))
    }

    complex match {
      case true =>
        /*val field = resource / Vocabulary.descriminatorfield match {
          case x: YAMLStringNode => x.string
          case _ => throw new DSLParseException(("Resource %s is complex, " +
            "but no descriminator field specified").format(name))
        }*/
        DSLComplexResource(name, unit, costpolicy)//, field)
      case false =>
        DSLSimpleResource(name, unit, costpolicy)
    }
  }

  /** Parse top level algorithm declarations */
  private def parseAlgorithms(algorithms: YAMLListNode,
                    resources: List[DSLResource],
                    results: List[DSLAlgorithm]): List[DSLAlgorithm] = {

    algorithms.head match {
      case YAMLEmptyNode => return List()
      case _ =>
    }

    val superName = algorithms.head / Vocabulary.overrides
    val algoTmpl = superName match {
      case y: YAMLStringNode =>
        results.find(p => p.name.equals(y.string)) match {
          case Some(x) => x
          case None => throw new DSLParseException("Cannot find super algorithm %s".format(superName))
        }
      case YAMLEmptyNode => DSLAlgorithm.emptyAlgorithm
      case _ => throw new DSLParseException("Super algorithm name %s not a string".format())
    }

    val algorithm = constructAlgorithm(algorithms.head.asInstanceOf[YAMLMapNode],
      algoTmpl, resources)

    val tmpresults = algorithm :: results
    algorithm :: parseAlgorithms(algorithms.tail, resources, tmpresults)
  }

  /** Construct an algorithm object from a yaml node*/
  def constructAlgorithm(algorithm: YAMLMapNode,
                      algoTmpl: DSLAlgorithm,
                      resources: List[DSLResource]): DSLAlgorithm = {
    val name = algorithm / Vocabulary.name match {
      case x: YAMLStringNode => x.string
      case YAMLEmptyNode => throw new DSLParseException("Algorithm does not have a name")
    }

    val overr = algorithm / Vocabulary.overrides match {
      case x: YAMLStringNode => Some(algoTmpl)
      case YAMLEmptyNode => None
    }

    val algos = resources.map {
      r =>
        val algo = algorithm / r.name match {
          case x: YAMLStringNode => x.string
          case y: YAMLIntNode => y.int.toString
          case YAMLEmptyNode => algoTmpl.equals(DSLAlgorithm.emptyAlgorithm) match {
            case false => algoTmpl.algorithms.getOrElse(r,
              throw new DSLParseException(("Superalgo does not specify an algorithm for resource:%s").format(r.name)))
            case true => throw new DSLParseException(("Cannot find calculation algorithm for resource %s in either " +
              "algorithm %s or a superalgorithm").format(r.name, name))
          }
        }
        Map(r -> algo)
    }.foldLeft(Map[DSLResource, String]())((x,y) => x ++ y)

    val timeframe = algorithm / Vocabulary.effective match {
      case x: YAMLMapNode => parseTimeFrame(x)
      case YAMLEmptyNode => algoTmpl.equals(DSLAlgorithm.emptyAlgorithm) match {
        case false => algoTmpl.effective
        case true => throw new DSLParseException(("Cannot find effectivity period for algorithm %s ").format(name))
      }
    }

    DSLAlgorithm(name, overr, algos, timeframe)
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
      case YAMLEmptyNode => DSLPriceList.emptyPriceList
      case _ => throw new DSLParseException("Super pricelist name %s not a string".format())
    }

    val pl = constructPriceList(pricelists.head.asInstanceOf[YAMLMapNode],
      tmpl, resources)

    val tmpresults = pl :: results
    pl :: parsePriceLists(pricelists.tail, resources, tmpresults)
  }

  /* Construct a pricelist from a YAML node and template, which may be
  * an empty pricelist or an inhereted pricelist definition.
  */
  def constructPriceList(pl: YAMLMapNode, tmpl: DSLPriceList,
                         resources: List[DSLResource]): DSLPriceList = {
    val name = pl / Vocabulary.name match {
      case x: YAMLStringNode => x.string
      case YAMLEmptyNode => throw new DSLParseException(
        "Pricelist does not have a name")
    }

    val overr = pl / Vocabulary.overrides match {
      case x: YAMLStringNode => Some(tmpl)
      case YAMLEmptyNode => None
    }

    val prices = resources.map {
      r =>
        val price = pl / r.name match {
          case y: YAMLIntNode => y.int.toDouble
          case z: YAMLDoubleNode => z.double.toDouble
          case a: YAMLStringNode => a.string.toDouble
          case YAMLEmptyNode => tmpl.equals(DSLAlgorithm.emptyAlgorithm) match {
            case false => tmpl.prices.getOrElse(r,
              throw new DSLParseException(("Superpricelist does not specify a price for resource:%s").format(r.name)))
            case true => throw new DSLParseException(("Cannot find price for resource %s in either pricelist %s or " +
              "its super pricelist").format(r.name, name))
          }
        }
        Map(r -> price)
    }.foldLeft(Map[DSLResource, Double]())((x, y) => x ++ y)

    val timeframe = pl / Vocabulary.effective match {
      case x: YAMLMapNode => parseTimeFrame(x)
      case YAMLEmptyNode => tmpl.equals(DSLAlgorithm.emptyAlgorithm) match {
        case false => tmpl.effective
        case true => throw new DSLParseException(("Cannot find effectivity period for pricelist %s").format(name))
      }
    }
    DSLPriceList(name, overr, prices, timeframe)
  }

  private def parseCreditPlans(creditsplans: YAMLListNode,
                               results: List[DSLCreditPlan]) : List[DSLCreditPlan] = {
    creditsplans.head match {
      case YAMLEmptyNode => return List()
      case _ =>
    }

    val superName = creditsplans.head / Vocabulary.overrides
    val tmpl = superName match {
      case y: YAMLStringNode =>
        results.find(p => p.name.equals(y.string)) match {
          case Some(x) => x
          case None => throw new DSLParseException("Cannot find super credit plan %s".format(superName))
        }
      case YAMLEmptyNode => DSLCreditPlan.emptyCreditPlan
      case _ => throw new DSLParseException("Super credit plan name %s not a string".format())
    }

    val plan = constructCreditPlan(creditsplans.head.asInstanceOf[YAMLMapNode], tmpl)

    val tmpresults = plan :: results
    plan :: parseCreditPlans(creditsplans.tail, tmpresults)
  }

  def constructCreditPlan(plan: YAMLMapNode, tmpl: DSLCreditPlan): DSLCreditPlan = {

    val name = plan / Vocabulary.name match {
      case x: YAMLStringNode => x.string
      case YAMLEmptyNode => throw new DSLParseException(
        "Credit plan does not have a name")
    }

    val overr = plan / Vocabulary.overrides match {
      case x: YAMLStringNode => Some(tmpl)
      case YAMLEmptyNode => None
    }

    val at = plan / Vocabulary.at match {
      case x: YAMLStringNode => parseCronString(x.string)
      case YAMLEmptyNode => throw new DSLParseException(
        "Credit plan does not define repetition specifier")
    }

    val atCron = (plan / Vocabulary.at).asInstanceOf[YAMLStringNode]

    val credits = plan / Vocabulary.credits match {
      case x: YAMLIntNode => x.int.toDouble
      case y: YAMLDoubleNode => y.double.toDouble
      case YAMLEmptyNode => throw new DSLParseException(
        "Credit plan does not have a name")
    }

    val timeframe = plan / Vocabulary.effective match {
      case x: YAMLMapNode => parseTimeFrame(x)
      case YAMLEmptyNode => tmpl.equals(DSLCreditPlan.emptyCreditPlan) match {
        case false => tmpl.effective
        case true => throw new DSLParseException(
          ("Cannot find effectivity period for creditplan %s").format(name))
      }
    }

    DSLCreditPlan(name, overr, credits, at, atCron.string, timeframe)
  }

  /** Parse top level agreements */
  private def parseAgreements(agreements: YAMLListNode,
                      policies: List[DSLAlgorithm],
                      pricelists: List[DSLPriceList],
                      resources: List[DSLResource],
                      creditplans: List[DSLCreditPlan],
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
       case YAMLEmptyNode => DSLAgreement.emptyAgreement
       case _ => throw new DSLParseException("Super agreement name %s not a string".format(superName))
     }

     val agr = constructAgreement(agreements.head.asInstanceOf[YAMLMapNode],
       tmpl, policies, pricelists, resources, creditplans)

     val tmpresults = agr :: results
     agr :: parseAgreements(agreements.tail, policies, pricelists,
       resources, creditplans, tmpresults)
   }

  def constructAgreement(agr: YAMLMapNode,
                         tmpl: DSLAgreement,
                         policies: List[DSLAlgorithm],
                         pricelists: List[DSLPriceList],
                         resources: List[DSLResource],
                         creditplans: List[DSLCreditPlan]) : DSLAgreement = {
     val name = agr / Vocabulary.name match {
      case x: YAMLStringNode => x.string
      case YAMLEmptyNode => throw new DSLParseException("Agreement does not have a name")
    }

    val algorithm = agr / Vocabulary.algorithm match {
      case x: YAMLStringNode => policies.find(p => p.name.equals(x.string)) match {
        case Some(y) => y
        case None => throw new DSLParseException(("Cannot find algorithm named %s").format(x))
      }
      case y: YAMLMapNode =>
        if (y / Vocabulary.name == YAMLEmptyNode)
          y.map += (Vocabulary.name -> YAMLStringNode("/","%s-algorithm".format(name)))
        constructAlgorithm(y, tmpl.algorithm, resources)
      case YAMLEmptyNode => tmpl.equals(DSLAgreement.emptyAgreement) match {
        case true => throw new DSLParseException(("No algorithm for agreement %s").format(name))
        case false => tmpl.algorithm
      }
    }

    val pricelist = agr / Vocabulary.pricelist match {
      case x: YAMLStringNode => pricelists.find(p => p.name.equals(x.string)) match {
        case Some(y) => y
        case None => throw new DSLParseException(("Cannot find pricelist named %s").format(x))
      }
      case y: YAMLMapNode =>
        if (y / Vocabulary.name == YAMLEmptyNode)
          y.map += ("name" -> YAMLStringNode("/","%s-pricelist".format(name)))
        constructPriceList(y, tmpl.pricelist, resources)
      case YAMLEmptyNode => tmpl.equals(DSLAgreement.emptyAgreement) match {
        case true => throw new DSLParseException(("No algorithm for agreement %s").format(name))
        case false => tmpl.pricelist
      }
    }

    val creditplan = agr / Vocabulary.creditplan match {
      case x: YAMLStringNode => creditplans.find(p => p.name.equals(x.string)) match {
        case Some(y) => y
        case None => throw new DSLParseException(("Cannot find crediplan named %s").format(x))
      }
      case y: YAMLMapNode =>
        if (y / Vocabulary.name == YAMLEmptyNode)
          y.map += ("name" -> YAMLStringNode("/","%s-creditplan".format(name)))
        constructCreditPlan(y, tmpl.creditplan)
      case YAMLEmptyNode => tmpl.equals(DSLAgreement.emptyAgreement) match {
        case true => throw new DSLParseException(("No creditplan for agreement %s").format(name))
        case false => tmpl.creditplan
      }
    }

    val overrides = tmpl.equals(DSLAgreement.emptyAgreement) match {
      case false => Some(tmpl)
      case true => None
    }

    DSLAgreement(name, overrides, algorithm, pricelist, creditplan)
  }

  /** Parse a timeframe declaration */
  def parseTimeFrame(timeframe: YAMLMapNode): DSLTimeFrame = {
    val from = timeframe / Vocabulary.from match {
      case x: YAMLIntNode => new Date(x.int.longValue * 1000L)
      case y: YAMLLongNode => new Date(y.long)
      case _ => throw new DSLParseException("No %s field for timeframe %s".format(Vocabulary.from, timeframe))
    }

    val to = timeframe / Vocabulary.to match {
      case x: YAMLIntNode => Some(new Date(x.int.longValue * 1000L))
      case y: YAMLLongNode => Some(new Date(y.long))
      case YAMLEmptyNode => None
    }

    val effective = timeframe / Vocabulary.repeat match {
      case x: YAMLListNode => parseTimeFrameRepeat(x)
      case YAMLEmptyNode => List()
    }

    DSLTimeFrame(from, to, effective)
  }

  /** Parse a resource frame repeat block */
  def parseTimeFrameRepeat(tmr: YAMLListNode): List[DSLTimeFrameRepeat] = {

    if (tmr.isEmpty)
      return List()

    /** Parse a resource frame entry (start, end tags) */
    def findInMap(repeat: YAMLMapNode,
                  tag: String) : (String, List[DSLTimeSpec]) = {
      repeat / tag match {
        case x: YAMLStringNode => (x.string, parseCronString(x.string))
        case YAMLEmptyNode => throw new DSLParseException("No %s field for repeat entry %s".format(tag, repeat))
      }
    }

    val start = findInMap(tmr.head.asInstanceOf[YAMLMapNode], Vocabulary.start)
    val end = findInMap(tmr.head.asInstanceOf[YAMLMapNode], Vocabulary.end)

    DSLTimeFrameRepeat(start._2, end._2, start._1,end._1) :: parseTimeFrameRepeat(tmr.tail)
  }

  /** 
   * Wraps the [[http://kenai.com/projects/crontab-parser/pages/Home crontabparser]]
   * library to parse crontab-like strings. The input format differs from the
   * [[http://en.wikipedia.org/wiki/Cron default cron format]] in the following ways:
   *
   *  - Only 5 field cron specs are allowed
   *  - Multiple values per field (e.g. Mon,Wed,Fri) are not allowed. Ranges
   *    (e.g. Mon-Fri) are however allowed.
   */
  def parseCronString(input: String): List[DSLTimeSpec] = {

    if (input.split(" ").length != 5)
      throw new DSLParseException("Only five-field cron strings allowed: " + input)

    val cron = try {
      asScalaBuffer(CronTabParserBridge.parse(input))
    } catch {
      case e => throw new DSLParseException("Error parsing cron string: " + e.getMessage)
    }
    def splitMultiVals(ii : Int): List[Int] = {
      val input = cron.get(ii).toString
      if (input.equals("*"))
        (-1).until(0).toList
      else if (input.contains('-')) {
        val ints = input.split('-')
        ints(0).toInt.until(ints(1).toInt + 1).toList
      } else if (input.contains(','))
        input.split(',').map(_.toInt).toList
       else
        input.toInt.until(input.toInt + 1).toList
    }
    for { a <- splitMultiVals(0)
          b <- splitMultiVals(1)
          c <- splitMultiVals(2)
          d <- splitMultiVals(3)
          e <- splitMultiVals(4)  } yield
      DSLTimeSpec(a,b,c,d,e)
  }
}

/** Exception thrown when a parsing error occurs*/
class DSLParseException(msg: String) extends Exception(msg)
