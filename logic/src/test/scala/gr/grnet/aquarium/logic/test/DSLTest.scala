package gr.grnet.aquarium.logic.test

import org.junit.Test
import org.junit.Assert._
import gr.grnet.aquarium.logic.accounting.dsl._

class DSLTest {

  @Test
  def testDSLLoad = {
    var policy = DSL.parse(
      getClass.getClassLoader.getResourceAsStream("policy.yaml")
    )
    assertNotNull(policy)
  }

  @Test
  def testMergePolicies = {
    val vm = DSLResource("vmtime")
    val bup = DSLResource("bup")
    val bdown = DSLResource("bdown")

    val a = DSLPolicy("1", "2", Map(vm -> "abc", bup -> "def"), null)
    val b = DSLPolicy("2", "", Map(vm -> "xyz", bdown -> "foo"), null)

    val result = DSL.mergePolicy(a, b)

    assertEquals(result.name, "2")
    assertEquals(result.algorithms.size, 3)
    assertEquals(result.algorithms.get(vm), Some("abc"))
    assertEquals(result.algorithms.get(bup), Some("def"))
    assertEquals(result.algorithms.get(bdown), Some("foo"))
  }
}