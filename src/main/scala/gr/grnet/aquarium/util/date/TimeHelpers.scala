package gr.grnet.aquarium.util.date

import java.util.Date


/**
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>
 */

object TimeHelpers {
  def nowMillis = System.currentTimeMillis()

  def nowDate = new Date(nowMillis)
}