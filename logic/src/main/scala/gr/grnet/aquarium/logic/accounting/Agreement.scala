package gr.grnet.aquarium.logic.accounting

import collection.immutable.HashMap
import collection.immutable.List
import java.util.Date

abstract class Agreement {

  val id : Long = 0

  val pricelist : Map[AccountingEventType.Value, Float] = Map()

  var policies : Map[AccountingEventType.Value, Map[Date,Policy]] = Map()

  def addPolicy(et: AccountingEventType.Value, p: Policy, d: Date) = {
    policies = policies ++ Map(et -> Map(d -> p))
  }

  /** Get applicable policy at the time that the event occured */
  def policy(et: AccountingEventType.Value, d: Date) : Option[Policy] = {
    val ruleset = policies.getOrElse(et, new HashMap[Date, Policy])
    val keyset = List(new Date(0)) ++
                 ruleset.keys.toList.sorted ++
                 List(new Date(Long.MaxValue))

    val key = intervals[Date](keyset).find(
      k => d.compareTo(k.head) >= 0 && d.compareTo(k.reverse.head) < 0
    ).get.head

    ruleset.get(key)
  }

  /**Generate a list of pairs by combining the elements of the provided
   * list sequentially.
   *
   * for example:
   * intervals(List(1,2,3,4)) -> List(List(1,2), List(2,3), List(3,4))
   */
  private def intervals[A](a : List[A]) : List[List[A]] = {
    if (a.size <= 1)
      return List()
    List(List(a.head, a.tail.head)) ++ intervals(a.tail)
  }
}