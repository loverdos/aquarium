package gr.grnet.aquarium.util.yaml

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class YAMLListNode(path: String,  list: List[YAMLNode]) extends YAMLNode {
  def /(childName: String) = YAMLEmptyNode

  override def listValue = list
  override def isList = true

  def head = list match {
    case Nil    => YAMLEmptyNode
    case h :: _ => h
  }
  def tail = YAMLListNode(path + "::tail", list.tail)

  override def isEmpty = list.isEmpty

  override def foreach[T](f: (YAMLNode) => T) = {
    for(node <- listValue) {
      f(node)
    }
  }

  def withPath(newPath: String) = this.copy(path = newPath)
}