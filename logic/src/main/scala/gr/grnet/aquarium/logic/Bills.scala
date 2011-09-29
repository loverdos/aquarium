package gr.grnet.aquarium.logic

import gr.grnet.aquarium.model._
import collection.JavaConversions._
import java.util.{Date}
import collection.immutable.HashSet

trait Bills {

  def calcBill(e: Entity) : Float = calcBill(e, None, None, None)

  /**
   * Calculate the bills for all resources used by the user
   *
   * @param e The entity to calculate the bill for
   * @param res The resource to calculate the bill for.
   * @param from The date to start the calculation from.
   * @param to The date up to which the calculation should be done.
   */
  def calcBill(e: Entity, res: Option[ServiceItem],
               from: Option[Date], to: Option[Date]): Float = {

    val items = res match {
      case Some(x) => (new HashSet[ServiceItem]) + x
      case None => asScalaSet(e.serviceItems)
    }

    items.map {
      si => asScalaSet(si.configItems).map {
        ci => asScalaSet(ci.runtime).filter {
          rt => {
            rt.timestamp.getTime >= from.getOrElse(new Date(0L)).getTime &&
            rt.timestamp.getTime <= to.getOrElse(new Date()).getTime
          }
        }.map {
          rt => ci.resource.cost * rt.measurement
        }.fold(0F) { // Per config item
          (total, cost) => total + cost
        }
      }.fold(0F) { // Per service item
        (total, cost) => total + cost
      }
    }.fold(0F) { // Per entity
      (total, cost) => total + cost
    }
  }
}