package gr.grnet.aquarium.logic.test

import gr.grnet.aquarium.model._
import scala.io.Source
import scala.util.parsing.json._

/** Loads [[https://docs.djangoproject.com/en/dev/howto/initial-data/ Django-style]]
  *  fixtures in the database. It expects an open entity manager.
  *
  * @author Georgios Gousios <gousiosg@grnet.gr>
  */
trait FixtureLoader {

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
      ref.getClass.getMethods.find(
        _.getName == name + "_$eq"
      ).get.invoke(ref, value.asInstanceOf[AnyRef])
    }
  }

  /** Loads a fixture from classpath
    *
    * @param fixture The fixture path to load the test data from. It should
    *                be relative to the classpath root.  
    */
  def loadFixture(fixture : String) {

    val json = Source.fromInputStream(
      getClass.getClassLoader.getResourceAsStream(fixture)
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

  private def addRecord(record: Map[String, Any]): Unit = {
    //Top level record fields
    val model = record.get("model").get.toString()
    val fields = record.get("fields").get
    val id = record.get("pk").getOrElse(
      throw new Exception("The pk field is missing from record: " + record)
    ).asInstanceOf[Double].longValue()

    val fieldMap = fields match {
      case v: Map[_, _] => fields.asInstanceOf[Map[String, Any]]
      case _ => throw new Exception("Not supported: ".concat(fields.toString))
    }

    //Create an object instance
    val obj = loadClass[AnyRef](model).newInstance()

    //Set the field values
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
    DB.persistAndFlush(obj)

    /* Special treatment for the ID field: we allow JPA to set on persist, but
     * we reset it here to what the fixture specifies. This is to bypass
     * JPA's strict 'no-primary-key-updates' rule
     */
    updatePK(obj.getClass.getSimpleName, obj.getV("id").asInstanceOf[Long], id)
    DB.flush()
  }

  private def updatePK(entity : String, oldid: Long, newid : Long) = {
    val q = DB.createQuery(
      "update " + entity +
      " set id=:newid where id = :oldid")
    q.setParameter("newid", newid)
    q.setParameter("oldid", oldid)
    q.executeUpdate()
  }

  /* The scala JSON parser returns:
  *   -Strings for values in quotes
  *   -Doubles if the value is a number, not in quotes
  *
  *   The following methods do type conversions by hand
  */
  private def getDoubleValue(dbField: Any, value: Double) = {
    dbField match {
      case s if dbField.isInstanceOf[String] => value.toString
      case l if dbField.isInstanceOf[Long] => value.longValue
      case x if dbField.isInstanceOf[Int] => value.intValue
      case f if dbField.isInstanceOf[Float] => value.floatValue
      case d if dbField.isInstanceOf[Double] => value
    }
  }

  private def getStringValue(dbField: Any, value: String) = {
    dbField match {
      case s if dbField.isInstanceOf[String] => value
      case l if dbField.isInstanceOf[Long] => value.toLong
      case x if dbField.isInstanceOf[Int] => value.toInt
      case f if dbField.isInstanceOf[Float] => value.toFloat
      case d if dbField.isInstanceOf[Double] => value
    }
  }

  private def getFieldValue(field: Any, value: Any) = {
    value match {
      case v: Double => getDoubleValue(field, value.asInstanceOf[Double])
      case v: String => getStringValue(field, value.asInstanceOf[String])
      case _ => throw new Exception("Unsupported value type: " + value.toString)
    }
  }
}