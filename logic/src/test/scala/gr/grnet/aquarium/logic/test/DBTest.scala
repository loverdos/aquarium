package gr.grnet.aquarium.logic.test

import org.junit.{AfterClass, BeforeClass}
import org.dbunit._
import database.DatabaseSequenceFilter
import dataset.FilteredDataSet
import dataset.xml.{FlatXmlDataSetBuilder, FlatXmlDataSet, XmlDataSet}
import operation.DatabaseOperation
import xml._
import gr.grnet.aquarium.model._

object DBTest {

  @BeforeClass
  def before() {

    val xml = XML.load(getClass.getResourceAsStream("/META-INF/persistence.xml"))

    def getConfig(value : String) = {
      println("Searching for:" + value)
      val a = xml.descendant_or_self.filter {
        node => (node \ "@name").text == value
      }.head

      a.attribute("value").get.text
    }

    val driver = getConfig ("javax.persistence.jdbc.driver")
    val uname = getConfig ("javax.persistence.jdbc.user")
    val passwd = getConfig ("javax.persistence.jdbc.password")
    val url = getConfig("javax.persistence.jdbc.url")

    // Make sure the DB schema is there
    if (!DB.isOpen())
      DB.openEM()

    val tester = new JdbcDatabaseTester(driver, url, uname, passwd)
    val filter = new DatabaseSequenceFilter(tester.getConnection)
    
    val ds = new FilteredDataSet(filter,
      new FlatXmlDataSetBuilder().build(getClass.getResourceAsStream("/testdata.xml")))
    DatabaseOperation.REFRESH.execute(tester.getConnection, ds)

  }

  @AfterClass
  def after() {
    
  }
}