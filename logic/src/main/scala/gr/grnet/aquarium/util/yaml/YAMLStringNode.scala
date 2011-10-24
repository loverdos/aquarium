package gr.grnet.aquarium.util.yaml

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class YAMLStringNode(string: String) extends YAMLNode {
  def /(childName: String) = YAMLEmptyNode

  override def stringValue = string

  override def isString = true
}
