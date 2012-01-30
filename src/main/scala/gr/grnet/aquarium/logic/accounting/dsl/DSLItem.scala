package gr.grnet.aquarium.logic.accounting.dsl

import gr.grnet.aquarium.util.yaml.YAMLHelpers

import gr.grnet.aquarium.util.CollectionUtils


/**
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

abstract class DSLItem  {
  def toMap(): Map[String,  Any] =
    (Map[String, Any]() /: this.getClass.getDeclaredFields) {
      (a, f) =>
        f.setAccessible(true)
        a + (f.getName -> f.get(this))
    }

  def toYAML: String = YAMLHelpers.dumpYAML(toMap)
}