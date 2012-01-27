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

package gr.grnet.aquarium.util

import java.util.{Map => JMap,
                  List => JList,
                  HashMap => JHashMap,
                  ArrayList => JArrayList}

/**
 * Utility functions for Collections, including recursive conversion to Java
 * for Maps and Lists
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
trait CollectionUtils {

  /**Merge input maps on a field by field basis. In case of duplicate keys
   *  values from the first map are prefered.
   */
  private def mergeMaps[A, B](a: Map[A, B], b: Map[A, B]): Map[A, B] = {
    a ++ b.map {
      case (k, v) => k -> (a.getOrElse(k, v))
    }
  }

  /**Merge input maps on a field by field basis. In case of duplicate keys,
   *  the provided function is used to determine which value to keep in the
   *  merged map.
   */
  private def mergeMaps[A, B](ms: List[Map[A, B]])(f: (B, B) => B): Map[A, B] =
    (Map[A, B]() /: (for (m <- ms; kv <- m) yield kv)) {
      (a, kv) =>
        a + (if (a.contains(kv._1))
          kv._1 -> f(a(kv._1), kv._2)
        else kv)
    }


  /**
   * Recursively convert a Scala[String, Any] map to a Java equivalent.
   * It will also convert values that are Scala Maps or Lists or Seqs
   * to Java equivalents.
   */
  def mapToJavaMap(map: Map[String, Any]): JMap[String, Object] = {
    val result = new JHashMap[String, Object]

    map.foreach(kv => result.put(kv._1, transformValue(kv._2)))
    result
  }

  /**
   * Recursively convert a Scala List[Any] map to a Java equivalent
   * List[Object]. If the list's items are of type List[_]
   * and/or Map[String, Any] those will also be converted to
   * Java equivalents.
   */
  def listToJavaList(seq: List[Any]): JList[Object] = {
    val list = new JArrayList[Object]
    seq.foreach(a => list.add(transformValue(a)))
    list
  }

  private def transformValue(v: Any) = {
    val v2  = v match {
      case l: List[_]     => listToJavaList(l)
      case s: Seq[_]      => listToJavaList(s.toList)
      case i: Iterator[_] => listToJavaList(i.toList)
      case m: Map[String, Any] => mapToJavaMap(m)
      case _              => v
    }

    v2.asInstanceOf[Object]
  }
}