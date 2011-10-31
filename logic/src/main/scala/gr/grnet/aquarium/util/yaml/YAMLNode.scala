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

  def name = path.substring(path.lastIndexOf('/') + 1)
  
  def path: String
  def withPath(newPath: String): YAMLNode

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

  def foreach[T](f: YAMLNode => T): Unit = {}
}

/**
 * Companion object.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
object YAMLNode {
  def concatPaths(parent: String, child: String) = {
    if(parent == "/") {
      if(child startsWith "/") {
        child
      } else {
        "/" + child
      }
    } else {
      parent + "/" + child
    }
  }

  def indexedPath(basePath: String, index: Int) = "%s[%s]".format(basePath, index)

  def apply(obj: AnyRef, basePath: String = "/"): YAMLNode = {
    obj match {
      case null =>
        YAMLEmptyNode
      case javaMap: JMap[_, _] =>
        val scalaMap: mutable.Map[String, AnyRef] = javaMap.asInstanceOf[JMap[String, AnyRef]]
        val nodeMap = scalaMap map {
          case (key, value) if value.isInstanceOf[YAMLNode] =>
            val yvalue = value.asInstanceOf[YAMLNode]//.withPath(concatPaths(basePath, key))
            (key, yvalue)
          case (key, value) =>
            (key, apply(value, concatPaths(basePath, key)))
        }
        YAMLMapNode(basePath, nodeMap)
      case javaList: JList[_] =>
        val scalaList: mutable.Buffer[AnyRef] = javaList.asInstanceOf[JList[AnyRef]]
        val nodeList = scalaList.zipWithIndex.map { case (elem, index) => apply(elem, indexedPath(basePath, index)) }.toList
        YAMLListNode(basePath, nodeList)
      case string: String =>
        YAMLStringNode(basePath, string)
      case x: YAMLNode => x
      case int: java.lang.Integer =>
        YAMLIntNode(basePath, int)
      case double: java.lang.Double =>
        YAMLDoubleNode(basePath, double)
      case obj =>
        YAMLUnknownNode(obj, obj.getClass.getName)
    }
  }
}




