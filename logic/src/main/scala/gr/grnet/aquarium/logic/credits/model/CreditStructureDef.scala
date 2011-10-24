package gr.grnet.aquarium.logic.credits.model

/**
 * The definition of a credit structure.
 * This could mimic the organizational structure of the client though something like that is not necessary.
 *
 * CreditStructures are top-level and system-wide definitions that can be reused.
 *
 * We also support single inheritance.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class CreditStructureDef(id: String, name: String, units: List[CreditUnit], inherits: Option[String])
