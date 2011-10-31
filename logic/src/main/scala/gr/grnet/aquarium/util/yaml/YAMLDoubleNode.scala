package gr.grnet.aquarium.util.yaml

/**
 * 
 * @author Georgios Gousios <gousiosg@gmail.com>.
 */
case class YAMLDoubleNode(double: Double) extends YAMLNode {
  def /(childName: String) = YAMLEmptyNode

  override def doubleValue = double

  override def isDouble = true
}
