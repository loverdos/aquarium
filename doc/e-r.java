/*
 * Copyright 2011 GRNET S.A. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 *   1. Redistributions of source code must retain the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer.
 *
 *   2. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY GRNET S.A. ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GRNET S.A OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and
 * documentation are those of the authors and should not be
 * interpreted as representing official policies, either expressed
 * or implied, of GRNET S.A.
 */

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


