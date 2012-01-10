package gr.grnet.aquarium.user

import gr.grnet.aquarium.logic.test.DSLTest
import gr.grnet.aquarium.logic.accounting.Policy
import org.junit.Test

/**
 * 
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

class UserActorTest extends DSLTest {

  @Test
  def testUserStateSerialization = {
    before
    val now = System.currentTimeMillis()
    val state = UserState(
      "1",
      ActiveSuspendedSnapshot(true, now),
      CreditSnapshot(0, now),
      AgreementSnapshot("default", now),
      RolesSnapshot(Nil, now),
      PaymentOrdersSnapshot(Nil, now),
      OwnedGroupsSnapshot(Nil, now),
      GroupMembershipsSnapshot(Nil, now),
      OwnedResourcesSnapshot(Map(("foo", "1") -> (0F, 0L)), now)
    )

    println(state.toJson)
    assertNotThrows(state.toJson)
  }
}