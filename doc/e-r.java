/**
 * @opt operations
 * @opt attributes
 * @opt operations
 * @hidden
 */
class UMLOptions {}

/**
 * @assoc 1 - n ServiceTemplate
 * @assoc 1 - n ServiceItem
 */ 
public class Entity {
    public String name;
    public float credits;
}

public enum EntityType {
    User, Group
}

public class User extends Entity {
}

/** 
 * @composed m - n User
 * @assoc 0 - n Group
 */
public class Group extends Entity{}

/** 
 * @assoc m - n Group
 * @composed n - m User
 */
public class Organization extends Entity {
}

/**
 * @assoc 1 - n ServiceItem 
 * @has n - 1 ResourceType
 */
public class ServiceTemplate {
    public String name;
}

public class ServiceItem {
    public String url;
}

/**
 * @assoc n - 1 ServiceTemplate 
 */ 
public class Action{}

/**
 * @assoc n - 1 Action
 * @assoc n - 1 ServiceTemplate
 * @assoc n - 1 EntityType
 */
public class DefaultPermission {}

/**
 * @has n - 1 Entity
 * @composed n - 1 Action
 * @composed n - 1 ServiceItem
 */ 
public class Permission{}


/**
 * @assoc n - 1 Entity
 * @assoc n - 1 ServiceItem
 */ 
public class Limit{
    public String value;
}

/**
 * @assoc n - 1 ServiceItem 
 * @assoc n - 1 Entity
 */
public class Bill {
    public float cost;
    public Date date;
}

public class SynnefoVM extends ServiceItem {}
public class PithosFile extends ServiceItem{}
public class SaaSVM extends SynnefoVM {}


public enum ResourceType {
    CPU, RAM, DiskSpace, Bandwidth, License, Network
}

/**
 * @assoc n - 1 EntityType
 * @assoc n - 1 ServiceTemplate
 */
public class DefaultLimit {
    public float limit;
}


