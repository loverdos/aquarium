package gr.grnet.aquarium.util.yaml

import collection.mutable

/**
 * 
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
case class YAMLMapNode(map: mutable.Map[String, YAMLNode]) extends YAMLNode {
  def /(childName: String) = map.get(childName) match {
    case Some(child) => child
    case None => YAMLEmptyNode
  }

  override def mapValue = map.toMap // get an immutable version
  override def isMap = true
}
