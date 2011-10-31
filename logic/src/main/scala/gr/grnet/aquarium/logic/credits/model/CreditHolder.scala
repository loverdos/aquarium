package gr.grnet.aquarium.logic.credits.model

import java.net.URI


/**
 * A credit holder is the entity to which credits can be assigned.
 *
 * This can be the wallet.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
sealed trait CreditHolder {
  def name: String
  def label: String
  def isSingle: Boolean
  def isGroup: Boolean
  def members: List[String]
  def wallet: List[CreditAmount]
}

case class UserCreditHolder(name: String, label: String, wallet: List[CreditAmount]) extends CreditHolder {
  def isSingle = true
  def isGroup = false
  def members = Nil
}

case class GroupCreditHolder(name: String, label: String, members: List[String], wallet: List[CreditAmount]) extends CreditHolder {
  def isSingle = false
  def isGroup = true
}

