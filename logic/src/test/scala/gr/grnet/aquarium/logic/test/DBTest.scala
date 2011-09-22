package gr.grnet.aquarium.logic.test

import gr.grnet.aquarium.model._
import scala.io.Source
import scala.util.parsing.json._

trait DBTest {

  implicit def loadClass[T <: AnyRef](name: String): Class[T] = {
    val clazz = Class.forName(name, true, this.getClass.getClassLoader)
    clazz.asInstanceOf[Class[T]]
  }

  implicit def reflector(ref: AnyRef) = new {

    def getV(name: String): Any = {
      val field = ref.getClass.getMethods.find(_.getName == name)
      field match {
        case None => throw new Exception("No field: " + name +
                                         " for class:" + ref.getClass)
        case _ => field.get.invoke(ref)
      }
    }

    def setV(name: String, value: Any): Unit = {
      ref.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(ref, value.asInstanceOf[AnyRef])
    }
  }

  def loadFixture() {

    val json = Source.fromInputStream(
      getClass.getClassLoader.getResourceAsStream("data.json")
    ).mkString
    val data = JSON.parseFull(json).get

    data match {
      case x: List[Any] => x.foreach(
        f => f match {
          case y: Map[String, Any] => addRecord(y)
          case _ => throw new Exception("Not supported: ".concat(f.toString))
        })
      case _ => throw new Exception("Input JSON must be an array")
    }
  }

  def addRecord(record: Map[String, Any]): Unit = {
    //Top level record fields
    val model = record.get("model").get.toString()
    val fields = record.get("fields").get

    var fieldMap = fields match {
      case v: Map[_, _] => fields.asInstanceOf[Map[String, Any]]
      case _ => throw new Exception("Not supported: ".concat(fields.toString))
    }

    //The pk JSON field is mapped to the id field for all Aquarium objects
    //Add it to the field map to be processed the same way as other fields
     record.get("pk").get match {
       case None => println
       case _ => fieldMap += ("id" -> record.get("pk").get)
     }

    //Create an object instance
    val obj = loadClass[AnyRef](model).newInstance()

    //Set the remaining fields
    fieldMap.keys.foreach {
      k =>
        val classField = obj.getV(k)

        classField match {
          case null => throw new Exception ("Type for field: " + k +
            " in class: " + model + " is unknown (possibly: no default value" +
            " for field in declaring class)")
          case _ =>
        }

        val fieldEntry = fieldMap.get(k).get
        val typedValue = getFieldValue(classField, fieldEntry)
        obj.setV(k, typedValue)
    }

    //Save the object
    DB.merge(obj)
  }

  /* The scala JSON parser returns:
  *   -Strings for values in quotes
  *   -Doubles if the value is a number, not in quotes
  */
  def getDoubleValue(dbField: Any, value: Double) = {
    dbField match {
      case s if dbField.isInstanceOf[String] => value.toString
      case l if dbField.isInstanceOf[Long] => value.longValue
      case x if dbField.isInstanceOf[Int] => value.intValue
      case f if dbField.isInstanceOf[Float] => value.floatValue
      case d if dbField.isInstanceOf[Double] => value
    }
  }

  def getStringValue(dbField: Any, value: String) = {
    dbField match {
      case s if dbField.isInstanceOf[String] => value
      case l if dbField.isInstanceOf[Long] => value.toLong
      case x if dbField.isInstanceOf[Int] => value.toInt
      case f if dbField.isInstanceOf[Float] => value.toFloat
      case d if dbField.isInstanceOf[Double] => value
    }
  }

  def getFieldValue(field: Any, value: Any) = {
    value match {
      case v: Double => getDoubleValue(field, value.asInstanceOf[Double])
      case v: String => getStringValue(field, value.asInstanceOf[String])
      case _ => throw new Exception("Unsupported value type: " + value.toString)
    }
  }
}