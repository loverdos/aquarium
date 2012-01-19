package gr.grnet.aquarium.store

import gr.grnet.aquarium.logic.accounting.dsl.DSLPolicy
import com.ckkloverdos.maybe.Maybe

/**
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait PolicyStore {
  def loadPolicies(): List[DSLPolicy]
  def storePolicy(policy: DSLPolicy): Maybe[RecordID]
}