package gr.grnet.aquarium.util.yaml

/**
 * 
 * @author Georgios Gousios <gousiosg@gmail.com>.
 */
case class YAMLDoubleNode(path: String,  double: Double) extends YAMLNode {
  def /(childName: String) = YAMLEmptyNode

  override def doubleValue = double

  override def isDouble = true

  def withPath(newPath: String) = this.copy(path = newPath)
}
