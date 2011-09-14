package gr.grnet.aquarium.logic

import gr.grnet.aquarium.model._
import org.scala_libs.jpa.{ThreadLocalEM, LocalEMF}

object DB extends LocalEMF("aquarium", true) with ThreadLocalEM {}

trait Bills {

  def calc_bill(u : User) = {

  }
}