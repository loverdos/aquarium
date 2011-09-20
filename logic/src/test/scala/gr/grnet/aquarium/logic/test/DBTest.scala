package gr.grnet.aquarium.logic.test

import org.junit.{AfterClass, BeforeClass}
import gr.grnet.aquarium.model.DB
import org.dbunit._
import xml._

object DBTest {

  @BeforeClass
  def before() {

    val xml = XML.load(getClass.getResourceAsStream("/META-INF/persistence.xml"))

    def getConfig(value : String) = {
      println("Searching for:" + value) 
      val a = xml.descendant_or_self.filter {
        node => (node \ "name").text == value
      }.head

      a.attribute("value").get.text
    }

    val driver = getConfig ("javax.persistence.jdbc.driver")
    val uname = getConfig ("javax.persistence.jdbc.user")
    val passwd = getConfig ("javax.persistence.jdbc.password")
    val url = getConfig("javax.persistence.jdbc.url")

    println ("Driver:", driver, "\n")
    
    //Init JPA
    DB.getTransaction().begin()
    val tester = new JdbcDatabaseTester(driver, url, uname, passwd)

    DB.getTransaction().commit()
  }

  @AfterClass
  def after() {
    
  }
}