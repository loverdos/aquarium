package gr.grnet.aquarium.util.yaml

import java.util.{Map => JMap, List => JList}
import scala.collection.mutable
import scala.collection.JavaConversions._


/**
 * A representation of a parsed object that has originated in YAML format.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
trait YAMLNode {
  def /(childName: String): YAMLNode

  def intValue: Int = 0
  def doubleValue: Double = 0.0
  def stringValue: String = null
  def listValue: List[YAMLNode] = Nil
  def mapValue: Map[String, YAMLNode] = Map()

  def isEmpty = false

  def isString = false
  def isInt = false
  def isDouble = false
  def isMap = false
  def isList = false
  def isUnknown = false
}

/**
 * Companion object.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
object YAMLNode {
  def apply(obj: AnyRef): YAMLNode = {
    obj match {
      case null =>
        YAMLEmptyNode
      case map: JMap[_, _] =>
        val workingMap: mutable.Map[String, AnyRef] = map.asInstanceOf[JMap[String, AnyRef]]
        val mappedMap = workingMap map {
          case (key, value) if value.isInstanceOf[YAMLNode] =>
            (key, value).asInstanceOf[(String, YAMLNode)]
          case (key, value) =>
            (key, apply(value))
        }
        YAMLMapNode(mappedMap)
//      case map: mutable.Map[_, _] =>
//        val workingMap = map
//        val mappedMap = workingMap map {
//          case (key, value) if value.isInstanceOf[YAMLNode] =>
//            (key, value)
//          case (key, value) =>
//            (key, newYAMLNode(value))
//        }
//        YAMLMapNode(mappedMap)
      case list: JList[_] =>
        val workingList: mutable.Buffer[AnyRef] = list.asInstanceOf[JList[AnyRef]]
        val mappedList = workingList.map(apply(_)).toList
        YAMLListNode(mappedList)
      case string: String =>
        YAMLStringNode(string)
      case x: YAMLNode => x
      case int: java.lang.Integer =>
        YAMLIntNode(int)
      case double: java.lang.Double =>
        YAMLDoubleNode(double)
      case obj =>
        YAMLUnknownNode(obj, obj.getClass.getName)
    }
  }
}




