package gr.grnet.aquarium.logic.events

/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */
case class ProcessedResourceEvent(
    refId: String,       // ID of the resource event this one refers to
    refResource: String, // The resource name of the referred to event
    refOccurred: Long    // The millis when the referred to event occurred)
)