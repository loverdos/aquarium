package gr.grnet.aquarium.util.yaml

/**
 * 
 * @author Georgios Gousios <gousiosg@gmail.com>.
 */
case class YAMLIntNode(int: Int) extends YAMLNode {
  def /(childName: String) = YAMLEmptyNode

  override def intValue = int

  override def isInt = true
}
