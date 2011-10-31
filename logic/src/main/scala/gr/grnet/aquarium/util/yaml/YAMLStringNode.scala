package gr.grnet.aquarium.util.yaml

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class YAMLStringNode(path: String,  string: String) extends YAMLNode {
  def /(childName: String) = YAMLEmptyNode

  override def stringValue = string

  override def isString = true

  def withPath(newPath: String) = this.copy(path = newPath)
}
