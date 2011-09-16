package gr.grnet.aquarium.logic

import gr.grnet.aquarium.model._
import java.util.Date

trait Bills {

  def calcBill(e : Entity) = calcBill(e, None, None)

  /**
   * Calculate the bills for all resources used by the user
   *
   * @param e The entity to calculate the bill for
   * @param res The resource to calculate the bill for. If emtpy
   * @param from The date to start the calculation from. If empty, this means
   *             the date the last calculation was made
   * @param to The date up to which the calculation shoud be done. If empty, the
   *           current date is used.
   */
  def calcBill(e : Entity, res: Option[ServiceItem],
               from : Option[Date], to : Option[Date]) = {
    
  }

}