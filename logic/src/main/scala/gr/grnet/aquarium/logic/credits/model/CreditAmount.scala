package gr.grnet.aquarium.logic.credits.model

/**
 * The actual credit amount with info about its origin.
 *
 * Note that origin can inform us of the credit type too.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class CreditAmount(amount: Long, creditOrigin: CreditOrigin)