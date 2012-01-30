package gr.grnet.aquarium.store

import com.ckkloverdos.maybe.Maybe
import gr.grnet.aquarium.logic.events.PolicyEntry

/**
 * A store for serialized policy entries.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait PolicyStore {

  /**
   * Load all accounting policies valid after the specified time instance.
   * The results are returned sorted by PolicyEntry.validFrom
   */
  def loadPolicies(after: Long): List[PolicyEntry]

  /**
   * Store an accounting policy.
   */
  def storePolicy(policy: PolicyEntry): Maybe[RecordID]
}