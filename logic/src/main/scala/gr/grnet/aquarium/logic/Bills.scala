package gr.grnet.aquarium.logic

import gr.grnet.aquarium.model._
import collection.JavaConversions._
import collection.immutable.HashSet
import collection.mutable.Buffer
import java.util.Date

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
      si => cost(events(si, from, to))
    }.fold(0F) { // Per service item
          (total, cost) => total + cost
    }
  }

  def cost(events: Buffer[RuntimeData]) : Float = {
    
    0F
  }

  def events(item: ServiceItem, from: Option[Date],
             to: Option[Date]) : Buffer[RuntimeData] = {
    val q = DB.createNamedQuery[RuntimeData]("eventsPerItem")
    q.setParameter("srvItem", item)
    q.setParameter("from", from.getOrElse(new Date(0L)))
    q.setParameter("to", from.getOrElse(new Date()))
    q.findAll
  }
}