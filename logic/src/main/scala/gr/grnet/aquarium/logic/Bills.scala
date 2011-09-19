package gr.grnet.aquarium.logic

import gr.grnet.aquarium.model._
import java.util.Date
import collection.JavaConversions._

trait Bills {

  def calcBill(e: Entity) : Float = calcBill(e, None, None, None)

  /**
   * Calculate the bills for all resources used by the user
   *
   * @param e The entity to calculate the bill for
   * @param res The resource to calculate the bill for. If emtpy
   * @param from The date to start the calculation from.
   * @param to The date up to which the calculation should be done.
   */
  def calcBill(e: Entity, res: Option[ServiceItem],
               from: Option[Date], to: Option[Date]): Float = {

    asScalaSet(e.serviceItems).map {
      si => asScalaSet(si.configItems).map {
        ci => asScalaSet(ci.runtime).filter {
          rt => {
            rt.timestamp.getTime >= from.getOrElse(new Date(0L)).getTime &&
            rt.timestamp.getTime <= to.getOrElse(new Date()).getTime
          }
        }.map {
          rt => ci.resource.cost * rt.measurement
        }.reduce { // Per config item
          (cost, acc) => acc + cost
        }
      }.reduce { // Per service item
        (cost, acc) => acc + cost
      }
    }.reduce { // Per entity
      (cost, acc) => acc + cost
    }
  }
}