package gr.grnet.aquarium.util

import org.slf4j.LoggerFactory

/**
 * Mix this trait in your class to automatically get a Logger instance.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
trait Loggable {
  protected val logger = LoggerFactory.getLogger(getClass)
}