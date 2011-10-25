package gr.grnet.aquarium.logic.test

import org.junit.Test
import org.junit.Assert._
import gr.grnet.aquarium.logic.accounting.dsl._

class DSLTest {

  @Test
  def testDSLLoad = {
    val policy = DSL.parse(
      getClass.getClassLoader.getResourceAsStream("policy.yaml")
    )
    assertNotNull(policy)
  }

  @Test
  def testMergePolicies = {
    val vm = DSLResource("vmtime")
    val bup = DSLResource("bup")
    val bdown = DSLResource("bdown")

    val a = DSLPolicy("1", Some("2"), Map(vm -> "abc", bup -> "def"), null)
    val b = DSLPolicy("2", Some(""), Map(vm -> "xyz", bdown -> "foo"), null)

    val result = DSL.mergePolicy(a, b)

    assertEquals(result.name, "2")
    assertEquals(result.algorithms.size, 3)
    assertEquals(result.algorithms.get(vm), Some("abc"))
    assertEquals(result.algorithms.get(bup), Some("def"))
    assertEquals(result.algorithms.get(bdown), Some("foo"))
  }

  @Test
  def testCronParse = {
    var input = "12 * * * *"
    var output = DSL.parseCronString(input)
    assertEquals(output, List(DSLCronSpec(12, -1, -1, -1, -1)))

    input = "12 4 3 jaN-ApR *"
    output = DSL.parseCronString(input)
    assertEquals(output.size, 4)
    assertEquals(output(2), DSLCronSpec(12, 4, 3, 3, -1))

    input = "12 4 foo jaN-ApR *"
    assertThrows(DSL.parseCronString(input))

    input = "@midnight"
    assertThrows(DSL.parseCronString(input))
  }

  def assertThrows(f: => Unit) = {
    try {
      f
      assert(false)
    } catch {
      case e: Exception => assert(true)
    }
  }
}