package gr.grnet.aquarium.logic.accounting

import java.util.Date
import collection.mutable.{HashMap, Map}
import gr.grnet.aquarium.logic.accounting.AccountingEventType

abstract class Agreement {

  var pricelist : Map[AccountingEventType.Value, Float] = _

  var policies = new HashMap[AccountingEventType.Value, HashMap[Date,Policy]]

  def addPolicy(et: AccountingEventType.Value, p: Policy, d: Date) = {
    policies ++ Map(et -> Map(d -> p))
  }

  def policy(et: AccountingEventType.Value, d: Date) : Option[Policy] = {
    val ruleset = policies.getOrElse(et, new HashMap[Date, Policy])
    val key = ruleset.keys.toList.sorted.find(k => k.compareTo(d) >= 0).getOrElse(new Date())
    ruleset.get(key)
  }
}