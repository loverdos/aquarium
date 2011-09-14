package gr.grnet.aquarium.logic

import org.scala_libs.jpa.{ThreadLocalEM, LocalEMF}

object DB extends LocalEMF("aquarium", true) with ThreadLocalEM {}
