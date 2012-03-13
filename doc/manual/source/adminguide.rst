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

Aquarium is configured 

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

Document Revisions
------------------

==================    ================================
Revision              Description
==================    ================================
0.1 (Mar 2012)        Configuration options, running
==================    ================================


