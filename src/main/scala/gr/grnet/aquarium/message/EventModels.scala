package gr.grnet.aquarium.message

/**
 * Created with IntelliJ IDEA.
 * User: pgerakios
 * Date: 9/13/12
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */


case class IMEventModel(
                         id : String,
                         clientID: String,
                         details : Map[String,String],
                         eventType : String,
                         eventVersion : String,
                         isActive : Boolean,
                         occurredMillis: Long,
                         receivedMillis: Long,
                         role: String,
                         userID:String

                         )
{}

case class ResourceEventModel(
                               id : String,
                               clientID: String,
                               details : Map[String,String],
                               resource:String,
                               eventVersion : String,
                               instanceID : String,
                               occurredMillis: Long,
                               receivedMillis: Long,
                               value : Double,
                               userID:String
)
{}
