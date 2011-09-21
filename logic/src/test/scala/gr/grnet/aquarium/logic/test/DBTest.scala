package gr.grnet.aquarium.logic.test

import gr.grnet.aquarium.model._
import scala.io.Source
import scala.util.parsing.json._

trait DBTest {

  implicit def string2Class[T <: AnyRef](name: String): Class[T] = {
    val clazz = Class.forName(name, true, this.getClass.getClassLoader)
    clazz.asInstanceOf[Class[T]]
  }

  implicit def reflector(ref: AnyRef) = new {
    def getV(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
    def setV(name: String, value: Any): Unit = ref.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(ref, value.asInstanceOf[AnyRef])
  }

  def loadFixture() {

    val json = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("data.json")).mkString
    val data = JSON.parseFull(json).get

    data match {
      case x : List[Any] => x.foreach(_ match {
        case y : Map[String, Any] => addRecord(y)
        case _ => throw new Exception("Not supported")
      })
      case _ => throw new Exception("Input JSON must be an array")
    }
}
  private def addRecord(record : Map[String, Any]) : Unit = {
    val model = record.get("model").get.toString()
    val id = record.get("pk").get
    val fields = record.get("fields").get

    val clazz = string2Class[AnyRef](model)
    val obj = clazz.newInstance()
    obj.setV("id", id)



    DB.merge(obj)
  }
}