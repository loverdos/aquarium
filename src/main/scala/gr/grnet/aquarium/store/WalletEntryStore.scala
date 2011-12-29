package gr.grnet.aquarium.store

import gr.grnet.aquarium.logic.events.{WalletEntry}
import java.util.Date
import com.ckkloverdos.maybe.Maybe

/**
 * A store for Wallet entries.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
trait WalletEntryStore {

  def storeWalletEntry(entry: WalletEntry): Maybe[RecordID]

  def findWalletEntryById(id: String): Maybe[WalletEntry]

  def findUserWalletEntries(userId: String): List[WalletEntry]

  def findUserWalletEntriesFromTo(userId: String, from: Date, to: Date): List[WalletEntry]
}