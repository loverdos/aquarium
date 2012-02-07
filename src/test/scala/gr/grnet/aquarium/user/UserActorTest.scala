package gr.grnet.aquarium.user

import org.junit.Test

/**
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */

class UserActorTest {

  @Test
  def testUserStateSerialization = {
    val now = System.currentTimeMillis()
    val state = UserState(
      "1",
      0,
      0L,
      false,
      null,
      Nil, Nil,Nil,
      0L,
      ActiveSuspendedSnapshot(true, now),
      CreditSnapshot(0, now),
      AgreementSnapshot(Agreement("default", now, -1) :: Nil, now),
      RolesSnapshot(Nil, now),
      OwnedResourcesSnapshot(ResourceInstanceSnapshot("foo", "1", 0.1F, 1) :: Nil, now)
    )

    val json = state.toJson
    println(json)
  }
}