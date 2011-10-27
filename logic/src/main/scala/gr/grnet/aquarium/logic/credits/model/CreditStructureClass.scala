package gr.grnet.aquarium.logic.credits.model

/**
 * The definition of a credit structure.
 * This could mimic the organizational structure of the client though something like that is not necessary.
 *
 * These are top-level and system-wide definitions that can be reused.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class CreditStructureClass(
    id: String,
    name: String,
    memberClasses: List[CreditHolderClass])
