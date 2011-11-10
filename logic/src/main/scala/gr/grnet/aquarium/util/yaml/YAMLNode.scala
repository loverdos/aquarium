/*
 * Copyright 2011 GRNET S.A. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 *   1. Redistributions of source code must retain the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer.
 *
 *   2. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY GRNET S.A. ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GRNET S.A OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and
 * documentation are those of the authors and should not be
 * interpreted as representing official policies, either expressed
 * or implied, of GRNET S.A.
 */

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




