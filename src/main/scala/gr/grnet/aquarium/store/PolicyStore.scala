package gr.grnet.aquarium.store

import com.ckkloverdos.maybe.Maybe
import gr.grnet.aquarium.logic.events.PolicyEntry

/**
 * A store for serialized policy entries.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait PolicyStore {
  def loadPolicies(after: Long): List[PolicyEntry]
  def storePolicy(policy: PolicyEntry): Maybe[RecordID]
}