package gr.grnet.aquarium.util.yaml

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class YAMLUnknownNode(unknownObj: AnyRef, actualType: String) extends YAMLNode {
  def /(childName: String) = this

  override def isUnknown = false
}
