Administration Guide
=====================

The administration guide includes details on how an Aquarium setup should
look like.

Installation and Configuration
------------------------------

Aquarium is distributed as a TAR archive or through its source code repository
from `project home location <https://code.grnet.gr/projects/aquarium>`_.  For
instructions on how to build Aquarium from source, checkout the Development
Guide.

When unarchiving the binary archive, the following directories will appear:

- ``bin``  Contains the ``aquarium.sh`` script, used to start/stop Aquarium
- ``lib``  Library dependencies and the Aquarium.jar file
- ``conf`` Configuration files
- ``logs`` Where the Aquarium log output goes by default

Software Dependencies
---------------------

Aquarium depends on the following software to run:

- `MongoDB <http://www.mongodb.org/>`_, ver >2. Aquarium uses MongoDB to store
  incoming messages (resource and user events) and also to store intermediate
  computation results, such as the user wallets.
- `RabbitMQ <http://rabbitmq.com>`_, ver >2.7. Aquarium sets up a configurable
  number of queues to retrieve resource events from external systems. 

See the following sections for more detailed configuration notes for each one
of the external software systems.

MongoDB configuration for Aquarium
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. IMPORTANT::
   The `Failover Configuration`_ must precede any additional configuration step,
   if data replication is a strong requirement.

Before starting Aquarium with MongoDB, you will need to create a data schema
and an account for Aquarium. To do so, execute the following commands

.. code-block:: bash

  > use aquarium
  > 


Aquarium automatically creates the following MongoDB collections upon first run:

- ``resevents``: Contains incoming resource events
- ``userstates``: Contains timestamped snapshots of the in memory data stored per
  each user
- ``userevents``: Contains incoming user events
- ``policyEntries``: Contains the history of changes to the applicable charging
  policy

Indexes
+++++++

It is advisable to create the following indexes to ensure fast querying of data
in MongoDB

==============  ==================================================
Collection      Indexes on fields
==============  ==================================================
resevents       id, userId
userstates      userId


==============  ==================================================


To create a MongoDB index, open a MongoDB shell and do the following:

.. code-block:: bash 

  >use aquarium
  >db.resevents.ensureIndex({userId:1})
  >db.resevents.getIndexes()

You can see more on MongoDB indexes 
`here <http://www.mongodb.org/display/DOCS/Indexes>`_.

Failover Configuration
++++++++++++++++++++++




RabbitMQ configuration for Aquarium
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Active-active

Running Aquarium
----------------

To run Aquarium, change the current directory to the checked out and 

``./bin/aquarium.sh start``

Aquarium can also be started in debug mode, where all output is written to the
console and the JVM is started with the JPDA remote debugger intereface
listening to port 8000. An IDE can then be connected to ``localhost:8000`` 

``./bin/aquarium.sh debug``

To stop Aquarium in normal mode, run

``./bin/aquarium.sh stop``

The Aquarium start up script understands the following environment variables.
It is advised that for the time being you only change the JAVA_OPTS configuration
option.

==============  ==================================================
Variable        Description
==============  ==================================================
JAVA_OPTS       Runtime options for the JVM that runs Aquarium
AQUARIUM_PROP   Java system properties understood by Aquarium
AQUARIUM_OPTS   Cmd-line options for Aquarium
AQUARIUM_HOME   Location of the top level Aquarium dir
==============  ==================================================

Configuring Aquarium
--------------------

Aquarium is configured through the following configuration files:

-``aquarium.properties``: Is the central co
-``policy.yaml``
-``role-agreement.map``
-``log4j.conf``


- ``$AQUARIUM_HOME/conf/``
- ``$CWD``
- ``/etc/aquarium/``
- If searching in the above locations fails, Aquarium will use the default files
  provided in its classpath. This will probably cause Aquarium to fail.

The aquarium.properties file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

=============================== ================================== =============
Key                             Description                        Default value
=============================== ================================== =============
``aquarium.policy``             Location of the Aquarium           policy.yaml
                                accounting policy config file
``aquarium.role-agreement.map`` Location of the file that          role-agreement.map
                                defines the mappings between
``amqp.servers``                Comma separated list of AMQP       localhost
                                servers to use. To use more 
                                than one servers, they must be 
                                configured in active-active 
                                mode
``amqp.port``                   Port for connecting to the AMQP 
                                server
``amqp.username``               Username to connect to AMQP        aquarium
``amqp.passwd``                 Password to connect to AMQP        aquarium
``amqp.vhost``                  The vhost for the AMQP server      /
``amqp.resevents.queues``       Queue declarations for receiving  
                                resource events. Format is 
                                ``"exchange:routing.key"``.
                                Entries are separated by ``;``
``amqp.userevents.queues``      Queue declarations for receiving 
                                user events
``rest.port``                   REST service listening port        8080
``persistence.provider``        Provider for persistence services  mongo
``persistence.host``            Hostname for the persistence       localhost
                                service
``persistence.port``            Port for connecting to the         27017
                                persistence service
``persistence.username``        Username for connecting to the     mongo
                                persistence service
``persistence.password``        Password for connecting to the     mongo
                                persistence service
``mongo.connection.pool.size``  Maximum number of open             20
                                connections to MongoDB
=============================== ================================== =============

Document Revisions
------------------

==================    ================================
Revision              Description
==================    ================================
0.1 (Mar 2012)        Configuration options, running
==================    ================================


