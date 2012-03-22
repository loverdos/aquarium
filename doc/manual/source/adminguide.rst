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

Upon un-archiving the binary archive, the following directories will appear:

- ``bin``  Contains the ``aquarium.sh`` script, used to start/stop Aquarium
- ``lib``  Library dependencies and the Aquarium.jar file
- ``conf`` Configuration files
- ``logs`` Where the Aquarium log output goes by default

The steps required to configure Aquarium are in a nutshell the following:

1. Install and configure the `External Software Dependencies`_
2. Decide which external systems will be using Aquarium. Aquarium 
   requires one of each of the following two kinds of external systems:

   2.1 One or more identity providers, which provide details about the users that Aquarium will keep accounts for, through :ref:`user_event` s.

   2.2 One or more resource providers, which keep track of and monitor resource usage and inform Aquarium by sending :ref:`resource_event` s.
  
   For each external system, Aquarium requires a queue binding so that it
   can retrieve its messages. See more in the `Configuring Queues`_ section.

3. Configure `The aquarium.properties file`_ so that it points to the correct
   external systems.

4. `Make sure it works`_

External Software Dependencies
------------------------------

Aquarium depends on the following software to run:

- `MongoDB <http://www.mongodb.org/>`_, ver >2. Aquarium uses MongoDB to store
  incoming messages (resource and user events) and also to store intermediate
  computation results, such as the user wallets.
- `RabbitMQ <http://rabbitmq.com>`_, ver >2.7. Aquarium sets up a configurable
  number of queues to retrieve resource events from external systems.
- An external identity provider installation that is capable of publishing messages
  in the `User Event`_ format. Aquarium retrieves user events from a configured
  queue.

See the following sections for more detailed configuration notes for each one
of the external software systems.

MongoDB configuration for Aquarium
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. IMPORTANT::
   The `Failover Configuration`_ must precede any additional configuration step,
   if data replication is a strong requirement.

Before starting Aquarium with MongoDB, you will need to create a data schema
and an account for Aquarium. To do that, execute the following commands:

.. code-block:: bash

  $ mongo
  > use aquarium
  > db.addUser("aquarium", "aquarpasswd")
  > db.aquarium.users.find()

Aquarium automatically creates the following MongoDB collections when it 
first needs them:

- ``resevents``: Contains incoming resource events
- ``userstates``: Contains timestamped snapshots of the in memory data stored per
  each user
- ``userevents``: Contains incoming user events
- ``policyEntries``: Contains the history of changes to the applicable charging
  policy

Indexes
+++++++

It is advisable to create the following indexes to ensure fast querying of data
in MongoDB:

==============  ==================================================
Collection      Indexes on fields
==============  ==================================================
resevents       id, userId
userstates      userId
userevents      id, userId
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

MongoDB enables easy master/slave replication configuration, and it is
advisable to enable it in all Aquarium installations. To configure replication
for nodes A (IP: 10.0.0.1) and B (IP:10.0.0.2) with node A being the master do
the following:

1. Edit the MongoDB configuration file (``/etc/mongodb.conf`` on Debian) and include
   the following entries:

.. code-block:: bash

        directoryperdb = true
        replSet = aquarium-replicas

2. Login to MongoDB on the master node with the admin account: ``mongo A/admin``.
3. Enter the following configuration:

.. code-block:: bash

   >cfg = {
      _id : "aquarium-replicas",
      members : [
        {_id: 0, host: "10.0.0.1"},
        {_id: 1, host: "10.0.0.2"}
      ]
    }

   >rs.initiate(cfg)

4. Check that replication has started with: ``rs.status()``
5. Try to login to the aquarium database on both nodes: ``mongo A/aquarium`` and
   ``mongo B/aquarium``. On the master (A) the prompt will be ``PRIMARY>`` while
   on the slave (B) the prompt will be ``SECONDARY>``. 
6. Add a record to a test collection on the master: ``db.test.insert({'test':1})``. Go to the slave and type ``rs.slaveOk()`` and then ``db.test.find()``. You should see the entry just added. Remove the test collection from the master: ``db.test.drop()``.

You can find more on the
`MongoDB replication <http://www.mongodb.org/display/DOCS/Replication>`_ page

.. TIP::
   MongoDB also supports splitting the data on multiple nodes in a cluster on
   a per collection basis, using a pre-defined data key. This is called
   `sharding <http://www.mongodb.org/display/DOCS/Sharding+Introduction>`_,
   and is only recommended on installations with very high incoming data volumes,
   primarily for the ``resevents`` collection.

RabbitMQ configuration for Aquarium
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To connect to RabbitMQ, Aquarium needs an account with permission to create exchanges.
Such an account can be created as follows:

.. code-block:: bash

        rabbitmqctl add_user rabbit r@bb1t
        rabbitmqctl set_permissions -p / rabbit ".*" ".*" ".*"

To ensure high availability, Aquarium can be configured to use RabbitMQ in
active-active mode, where all nodes but one can fail individually. Aquarium has
been developed to automatically connect to the next available node in case a
connection to the currently enabled node fails.

To configure nodes A (IP: 10.0.0.1) and B (IP: 10.0.0.2) as a
RabbitMQ active-active cluster do the following:

1. Start RabbitMQ on both nodes and then stop it. On node A, look for a file named
   ``.erlang.cookie`` in RabbitMQ's runtime data directory (on Debian, this is
   configured to ``/var/lib/rabbitmq``). Copy its contents to the same file on node B
   and restart RabbitMQ on both nodes
2. On both node A and node B, run the following:

.. code-block:: bash

        rabbitmqctl add_user rabbit r@bb1t
        rabbitmqctl set_permissions -p / rabbit ".*" ".*" ".*"
        rabbitmqctl delete_user guest

This will create the same user with full administrative rights on both nodes and will
delete the default user.

3. On node A, run the following to initialize the cluster:

.. code-block:: bash

        rabbitmqctl stop_app
        rabbitmqctl reset
        rabbitmqctl cluster rabbit@10.0.0.1 rabbit@10.0.0.2
        rabbitmqctl start_app

4. To make sure it works, run: ``rabbitmqctl cluster_status``

To find out more, read the `RabbitMQ clustering guide <http://www.rabbitmq.com/clustering.html>`_.

Running Aquarium
----------------

To run Aquarium, change the current directory to the checked out and

``./bin/aquarium.sh start``

Aquarium can also be started in debug mode, where all output is written to the
console and the JVM is started with the JPDA remote debugger interface
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

- ``aquarium.properties``: This is the central configuration file. The following two
  files are directly linked from this.
- ``policy.yaml``: The file that contains the current resource charging policy.
- ``role-agreement.map``: Contains a map of user role names to agreements.
- ``log4j.conf``: Configuration for the Aquarium logger. See the Log4j
  `configuration instructions <http://logging.apache.org/log4j/1.2/manual.html>`_.

Upon initialization, Aquarium scans the following locations to discover the
first instance of the ``aquarium.properties`` file:

1. ``$AQUARIUM_HOME/conf/``
2. ``$CWD``
3. ``/etc/aquarium/``
4. If searching in the above locations fails, Aquarium will use the default files
   provided in its classpath. This will probably cause Aquarium to fail.

A brief description of the contents of each configuration file follows.

The aquarium.properties file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The following are the user configurable keys in the ``aquarium.properties`` file.

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
``amqp.resevents.queues``       Queue declarations for receiving   see below
                                resource events. Format is
                                ``"exchange:routing.key:queue"``
                                Entries are separated by ``;``
``amqp.userevents.queues``      Queue declarations for receiving   see below
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


Configuring queues
^^^^^^^^^^^^^^^^^^

The format for defining a queue mapping to retrieve messages from an AMQP
exchange is the following:

.. code-block:: bash

        exchange:routing.key:queue[;exchange:routing.key:queue]

This means that multiple queues can be declared per message type. The routing
key and exchange name must be agreed in advance with the external system that
provides the messages to it. For example, if Aquarium must be connected to its
project siblings (`Pithos <https://code.grnet.gr/projects/pithos>`_, `Cyclades
<https://code.grnet.gr/projects/synnefo/>`_), the following configuration must
be applied:

.. code-block:: bash

        pithos:pithos.resource.#:aquarium-pithos-resevents;cyclades:cyclades.resource.#:aquarium-cyclades-resevents


The policy.yaml file
^^^^^^^^^^^^^^^^^^^^^^^^

The ``policy.yaml`` file contains the description of the latest charging
policy, in the Aquarium DSL YAML format. You can find more details on the
Aquarium DSL in the `Development Guide`_.

Aquarium depends on the ``policy.yaml`` file to drive its resource charging
system, and for this reason it maintains a full history of the edits to it
internally. In fact, it even stores JSON renderings of the file in the
``policyEntries`` MongoDB collection. At startup, Aquarium will compare the
internally stored version time against the time the latest edit time of the
file on disk. If the file has been edited after the latest version stored in
the Aquarium database, the file is reloaded and a new policy version is stored.
All events whose validity time overlaps with the lifetimes of two (or more)
policies, will need to have separate charge entries according to the provisions
of each policy version. It is generally advised to keep changes to the policy
file to a minimum.

The role-agreement.map file
^^^^^^^^^^^^^^^^^^^^^^^^^^^

The ``role-agreement.map`` describes associations of user roles to agreements.

Associations are laid out one per line in the following format

.. code-block:: bash

        name-of-role=name-of-agreement

The allowed characters for both parts of the association are
``a-z,A-Z,0-9,-,_``, while lines that start with ``#`` are regarded as
comments. The names are case insensitive.

To cope with cases where a role is defined for a user, but Aquarium has not
been made aware of the change, a special entry starting with ``*``  is supported,
which assigns a default agreement to all unknown roles.
For example, the entry ``*=foobar``, assigns the agreement named ``foobar`` to
all roles not defined earlier on.

Currently, Aquarium does not keep a history of the ``role-agreement.map`` file,
as it does with the ``policy.yaml`` one.

Make sure it works
------------------

The processing in Aquarium is event-based.  When the system starts, not much
will happen until events show up on one of the configured queues. This 
however creates problems when Aquarium starts for the first time without
any external systems configured. For this reason, Aquarium comes with the
``bin/test.sh`` script that generates dummy resource and user events.

.. WARNING::
        Never run the ``test.sh`` script in a system that is already running
        in production, or you risk ending up with an inconsistent database.

To test an installation with the ``test.sh`` script, you can do the following:

* Make use the script is executable: ``chmod +x bin/test.sh``
* Start Aquarium in debug mode on a seperate terminal: ``./bin/aquarium.sh debug``
* Create 10 users: ``./bin/test.sh -u 10``. Check that Aquarium has gone through at least the following steps:

  1. Connected to the queue (search for ``FaultTolerantConnectionActor``)
  2. Read the policy.yaml file with no errors (look for ``Policy.scala``)
  3. Connected to MongoDB (look for ``Loaded StoreProvider``)
  4. User actors have been created without errors/exceptions
  5. MongoDB contains entries in both the ``userevents`` and ``userstates`` collections
  6. No messages are left unprocessed on RabbitMQ's queues ``rabbitmqctl list_queues``

Similarily, you can test the accounting system (and the correct loading of
the configured ``policy.yaml`` file) by producing dummy test messages with ``./bin/test.sh -r 10``.

To clean-up the database after testing, run the following in the mongo shell:

.. code-block:: bash

        > db.resevents.remove()
        > db.userevents.remove()
        > db.userstates.remove()
        > db.policyEntries.remove()


Document Revisions
------------------

==================    =========================================
Revision              Description
==================    =========================================
0.1 (Mar 2012)        Configuration options, running
0.2 (Mar 2012)        Instructions on how to config all files
==================    =========================================


