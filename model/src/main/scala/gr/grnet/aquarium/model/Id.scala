package gr.grnet.aquarium.model

import scala.reflect._
import javax.persistence.{Column, GeneratedValue, GenerationType}

trait Id {
  @javax.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  @BeanProperty
  var id: Long = 0

}