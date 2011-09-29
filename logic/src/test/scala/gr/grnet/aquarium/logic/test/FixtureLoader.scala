package gr.grnet.aquarium.logic.test

import gr.grnet.aquarium.model._
import scala.io.Source
import scala.util.parsing.json._
import java.lang.reflect.Field
import javax.persistence.{OneToMany, ManyToMany}
import java.util.{Date, Set}

/**Loads [[https://docs.djangoproject.com/en/dev/howto/initial-data/ Django-style]]
 *  fixtures in the database. It expects an open JPA entity manager. The code
 *  makes the following assumptions:
 *
 *  -All mapped types use the Id trait
 *  -{One,Many}ToMany relationships are mapped with a Set
 *  -The JSON parser returns either Map[String, A], List[A], String or Double
 *  (where A is again Double or String), for fields with subfields, arrays,
 *  strings and numbers respectively.
 *
 * @author Georgios Gousios <gousiosg@grnet.gr>
 */
trait FixtureLoader {

  /**Find and load a class in a type-safe manner*/
  implicit def loadClass[T <: AnyRef](name: String): Class[T] = {
    val clazz = Class.forName(name, true, this.getClass.getClassLoader)
    clazz.asInstanceOf[Class[T]]
  }

  /**Reflection helpers, to deal with Scala's low-level naming conventions*/
  implicit def reflector(ref: AnyRef) = new {

    /** Invoke the 'getter' method for a named field */
    def getV(name: String): Any = {
      val field = ref.getClass.getMethods.find(_.getName == name)
      field match {
        case None => throw new Exception("No field: " + name +
          " for class:" + ref.getClass)
        case _ => field.get.invoke(ref)
      }
    }

    /** Get the type for a field */
    def getT(name: String): Class[_] = {
      val field = ref.getClass.getMethods.find(_.getName == name)
      field match {
        case None => throw new Exception("No field: " + name +
          " for class:" + ref.getClass)
        case _ => field.get.getReturnType
      }
    }

    /** Invoke the 'setter' method for a field with the provided value*/
    def setV(name: String, value: Any): Unit = {
      ref.getClass.getMethods.find(
        p => p.getName == name + "_$eq"
      ).get.invoke(ref, value.asInstanceOf[AnyRef])
    }
  }

  /**Loads a fixture from classpath
   *
   * @param fixture The fixture path to load the test data from. It should
   *                be relative to the classpath root.
   */
  def loadFixture(fixture: String) {

    val json = Source.fromInputStream(
      getClass.getClassLoader.getResourceAsStream(fixture)
    ).mkString
    val data = JSON.parseFull(json).get
    System.err.print("Loading fixture " + fixture + ":")
    var i = 0

    data match {
      case x: List[Any] => x.foreach{
        f => f match {
          case y: Map[String, Any] => addRecord(y)
          case _ => throw new Exception("Not supported: ".concat(f.toString))
        }
        i += 1
        System.err.print(".")
      }
      case _ => throw new Exception("Input JSON must be an array")
    }
    System.err.println(i + " entries loaded")
  }

  /** Processes a single fixture record */
  private def addRecord(record: Map[String, Any]): Unit = {
    //Top level record fields
    val model = record.get("model").get.toString()
    val fields = record.get("fields").get
    val id = record.get("pk").getOrElse(
      throw new Exception("The pk field is missing from record: " + record)
    ).asInstanceOf[Double].longValue()

    //Other object fields, must be a Map[String, Any]
    val fieldMap = fields match {
      case v: Map[_, _] => fields.asInstanceOf[Map[String, Any]]
      case _ => throw new Exception("Not supported: ".concat(fields.toString))
    }

    //Create an object instance
    val tmpObj = loadClass[AnyRef](model).newInstance()
    //Save the object
    DB.persistAndFlush(tmpObj)

    /* Special treatment for the ID field: we allow JPA to set on persist, but
     * we reset it here to what the fixture specifies. This is to bypass
     * JPA's strict 'no-primary-key-updates' rule
     */
    updatePK(tmpObj.getClass.getSimpleName, tmpObj.getV("id").asInstanceOf[Long], id)

    val obj = DB.find(tmpObj.getClass, id).getOrElse(
      throw new Exception("Cannot find")
    ).asInstanceOf[AnyRef]

    //Set the field values
    fieldMap.keys.foreach {
      k =>
        var complex = false
        val typeof = obj.getT(k)

        /* The type the processed field prescribes its further treatment:
         *   -If it is a subclass of gr.grnet.aquarium.model.Id then the code
         *    considers it a reference to another model object
         *   -If it is a Set, then the code considers it an OneToMany or
         *    ManyToMany mapped field and adds the referenced object to the
         *    Set modeling the relation.
         *   -In all other cases, the field is considered as a raw value, which
         *    is set directly to it
         */
        typeof match {
          case null => throw new Exception("Type for field: " + k +
                " in class: " + model + " is unknown")
          case x if classOf[Id].isAssignableFrom(x) => complex = true
          case x if x.equals(classOf[java.util.Set[_]]) => complex = true
          case _ => complex = false
        }

        if (complex) {
          fieldMap.get(k).get match {
            case x: List[Double] => //{Many,One}ToMany relation
              setOneToMany(obj, k, fieldMap.get(k).get.asInstanceOf[List[Double]])
            case x: Double =>      //ManyToOne relation
              setManyToOne(obj, k, fieldMap.get(k).get.asInstanceOf[Double])
            case _ => throw new Exception("Not correct type (" + obj.getT(k) +
              ") for complex field: " + k +
              " (Accepted: Number or [Number])")
          }
        } else {
          val fieldEntry = fieldMap.get(k).get
          val typedValue = getFieldValue(obj.getV(k), fieldEntry)
          obj.setV(k, typedValue)
        }
    }

    DB.flush()
  }

  private def updatePK(entity: String, oldid: Long, newid: Long) = {
    val q = DB.createQuery(
      "update " + entity +
        " set id=:newid where id = :oldid")
    q.setParameter("newid", newid)
    q.setParameter("oldid", oldid)
    q.executeUpdate
  }

  /** Set the referenced object in a many to one relationship*/
  private def setManyToOne(dao: AnyRef, fieldName: String, fieldValue: Double) = {
    val other = DB.find(dao.getT(fieldName), fieldValue.longValue()).getOrElse(
      throw new Exception("Cannot find related object for " + dao.getClass +
        ", field: " + fieldName + " value: " + fieldValue)
    )
    dao.setV(fieldName, other)
  }

  /**Add the referenced field in a {one,many} to many relationship.
   * Uses the targetEntity field of either the OneToMany or the ManyToMany
   * JPA annotations to determine the type of the referenced entity, as the
   * type cannot be retrieved through the Set implementing the relationship
   * due to type erasure. 
   */
  private def setOneToMany(dao: AnyRef, fieldName: String,fieldValues: List[Double]) = {

    val field = findField(dao.getClass, fieldName).getOrElse(
      throw new Exception("Cannot find field:" + fieldName +
                          " for type:" + dao.getClass))

    val annot =  field.getAnnotations.find {
      f => f.annotationType() match {
          case x if x.equals(classOf[OneToMany]) => true
          case y if y.equals(classOf[ManyToMany]) => true
          case _ => false
        }
    }

    //Find the type of the referenced entity through the annotation
    val targetType = annot.get match {
      case x if annot.get.isInstanceOf[OneToMany] =>
        x.asInstanceOf[OneToMany].targetEntity()
      case y if annot.get.isInstanceOf[ManyToMany] =>
        y.asInstanceOf[ManyToMany].targetEntity()
      case _ => throw new Exception("No OneToMany or ManyToMany annotation" +
        "found on field:" + fieldName + " of type:" + dao.getClass +
        ", even though the fixture specifies multiple values for field")
    }

    //Add all values specified
    fieldValues.foreach {
      v =>
        val other = DB.find(targetType, v.longValue).getOrElse(
          throw new Exception("Cannot find entry of type "+ targetType +
            " with id=" + v.longValue)
        )
        // We make the assumption that *ToMany relationships are mapped by Sets
        val refField = dao.getV(fieldName).asInstanceOf[Set[Any]]
        refField.add(other)

        //Find and set the other type of a ManyToMany relationship
        if (annot.get.isInstanceOf[ManyToMany]) {
          findAnotField(targetType, dao.getClass) match {
            case Some(x) => other.asInstanceOf[AnyRef].getV(x.getName).asInstanceOf[Set[Any]].add(dao)
            case None => throw new Exception("Cannot find a field in class " +
              targetType + " that corresponds to field " + fieldName + " in " +
              "class " + dao.getClass
            )
          }
        }
    }
  }

  /** Get a named field reference in a class hierarchy */
  private def findField(clazz: Class[_], field: String) : Option[Field] = {
    if (clazz == null)
      return None

    val a = clazz.getDeclaredFields.find(f => f.getName.equals(field))
    a match {
      case Some(x) => a
      case None => findField(clazz.getSuperclass, field)
    }
  }

  /** Get a named field reference for a field whose ManyToMany or
   *  ManyToOne annotation property targetEntity matches the provided one.
   *  The search is done iteratively in the class hierarchy.
   */
  private def findAnotField(clazz: Class[_], target: Class[_]) : Option[Field] = {
    if (clazz == null)
      return None

    val a = clazz.getDeclaredFields.find {
      f =>
        val field = f.getAnnotations.find {
          a => a match {
            case y: ManyToMany => true
            case _ => false
          }
        }

        field match {
          case Some(x) => x.asInstanceOf[ManyToMany].targetEntity().equals(target)
          case None => false
        }
    }
    a match {
      case Some(x) => a
      case None => findAnotField(clazz.getSuperclass, target)
    }
  }

  /* The scala JSON parser returns:
   *   -Strings for values in quotes
   *   -Doubles if the value is a number, not in quotes
   *
   *   The following methods do type conversions manually
   */
  private def getDoubleValue(dbField: Any, value: Double) = {
    dbField match {
      case s if dbField.isInstanceOf[String] => value.toString
      case l if dbField.isInstanceOf[Long] => value.longValue
      case x if dbField.isInstanceOf[Int] => value.intValue
      case f if dbField.isInstanceOf[Float] => value.floatValue
      case d if dbField.isInstanceOf[Double] => value
      case d if dbField.isInstanceOf[Date] => new Date(value.longValue())
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
