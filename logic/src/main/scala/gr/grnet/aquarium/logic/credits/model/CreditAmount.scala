package gr.grnet.aquarium.logic.credits.model

/**
 * The actual credit amount with info about its origin.
 *
 * Note that origin can informa us of the credit type too.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class CreditAmount(amount: Long, creditOrigin: CreditOrigin)