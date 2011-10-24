package gr.grnet.aquarium.util.yaml

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case object YAMLEmptyNode extends YAMLNode {
  def /(childName: String) = YAMLEmptyNode

  override def isEmpty = true
}

