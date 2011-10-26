package gr.grnet.aquarium.logic.credits.model

/**
 * The actual credit amount with info about its type and origin.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class CreditAmount(amount: Long, creditType: CreditType, creditOrigin: CreditOrigin)