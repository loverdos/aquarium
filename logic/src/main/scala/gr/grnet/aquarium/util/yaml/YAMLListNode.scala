package gr.grnet.aquarium.util.yaml

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class YAMLListNode(list: List[YAMLNode]) extends YAMLNode {
  def /(childName: String) = YAMLEmptyNode

  override def listValue = list
  override def isList = true

  def head = list match {
    case Nil    => YAMLEmptyNode
    case h :: _ => h
  }
  def tail = YAMLListNode(list.tail)

  override def isEmpty = list.isEmpty
}