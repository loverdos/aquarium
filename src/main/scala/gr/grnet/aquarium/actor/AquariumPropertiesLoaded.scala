package gr.grnet.aquarium.actor

import com.ckkloverdos.props.Props

/**
 * This message is sent when the Aquarium properties are loaded.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

case class AquariumPropertiesLoaded(props: Props) extends ActorMessage