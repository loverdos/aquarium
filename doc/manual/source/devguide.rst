Aquarium Development Guide
==========================

The development guide includes descriptions of the APIs and extention points
offered by Aquarium. It also includes design and development setup information.

Overall architecture
--------------------

Aquarium's architectural design is mainly driven by two requirements: scaling
and fault tolerance. Aquarium's functionality is based on event sourcing.
`Event sourcing <http://en.wikipedia.org/wiki/Domain-driven_design>`_ 
assumes that all changes to application state are stored as a
sequence of events, in an immutable log. With such a log at hand, a system can
rebuild the current application state by replaying the events in order. The event
sourcing design pattern has some very interesting properties, which made it
particularity suitable for basing Aquarium on it:

- Multiple models can be used in order to process the events, concurrently. This means that Aquarium can provide a limited data view to its REST API and a more detailed one to a helpdesk frontend.

- It is possible to perform queries on past system states by stopping the event replay at a certain point of interest. This would prove very possible for a future debugging interface.

- In a carefully implemented event sourcing system, application crashes are not destructive, as long as event replay is fast enough and no state is inserted to the application without being recorded to the event log first.

- After event log replay, new events only cause updates in the system’s in-memory state, which can be done very fast.

Components
^^^^^^^^^^

.. image:: arch.png

An overview of the Aquarium architecture is presented in the figure above.  The
system is modeled as a collection of logically and functionally isolated
components, which communicate by message passing. Withing each component, a
number of actors take care of concurrently processing incoming messages through
a load balancer component which is the gateway to requests targeted to the
component. Each component is also monitored by its own supervisor; should an
actor fail, the supervisor will automatically restart it. The architecture
allows certain application paths to fail individually while the system is still
responsive, while also enabling future distribution of multiple components on
clusters of machines.

The system receives input mainly from two sources: a queue for resource and
user events and a REST API for credits and resource state queries. The queue
component reads messages from a configurable number of queues and persists them
in the application’s immutable log store. Both input components then forward
incoming messages to a network of dispatcher handlers which do not do any
processing by themselves, but know where the user actors lay. Actual processing
of billing events is done within the user actors. Finally, a separate network
of actors take care of scheduling periodic tasks, such as refiling of user
credits; it does so by issuing events to the appropriate queue.


The accounting system
----------------------

The accounting subsystem deals with charging users for services used and 
providing them with credits in order to be able to use the provided services.
As with the rest of the Aquarium, the architecture is open-ended: the accounting
system does not know in advance which services it supports or what resources
are being offered. The configuration of the accounting system is done
using a Domain Specific Language (DSL) described below. 

Data exchange with external systems is done through events, which are
persisted to an *immutable log*.

The accounting system is a generic event-processing engine that is configured by a
DSL. The DSL is mostly based on the
`YAML <http://en.wikipedia.org/wiki/Yaml>`_ 
format. The DSL supports limited algorithm definitions through integration of the Javascript language as defined below.

Glossary of Entities
^^^^^^^^^^^^^^^^^^^^

- *Credit*: A credit is the unit of currency used in Aquarium. It may or may not 
  correspond to real money.
- *Resource*: A resource represents an entity that can be charged for its usage. The 
  currently charged resources are: Time of VM usage, bytes uploaded and downloaded and bytes used for storage
- *Resource Event*: A resource event is generated from an external source and are permanently appended in an immutable event log. A raw event carries information about changes in an external system that could affect the status of a user's wallet.
- *AccountingEntry*: An accounting entry is the result of processing a resource event and is what gets stored to the user's wallet.
- *Price List*: A price list contains information of the cost of a resource. 
  A pricelist is only applied within a specified time frame.
- *Algorithm*: An algorithm specifies the way the charging calculation is done. It can be vary  depending on resource usage, time of raw event or other information.
- *Credit Plan*: Defines a periodic operation of refiling a user's wallet with a
  configurable amount of credits.
- *Agreement*: An agreement associates pricelists with algorithms and credit
  plans. An agreement is assigned to one or more users/credit holders.

Time frames
^^^^^^^^^^^

Time frames allow the specification of applicability periods for policies,
pricelists and agreements. A timeframe is by default continuous and has a
starting point; if there is no ending point, the timeframe is considered open
and its ending point is the time at the time of evaluation. 

A time frame definition can contain repeating time ranges that dissect it and
consequently constrain the applicability of the time frame to the defined
ranges only. A range always has a start and end point. A range is repeated
within a timeframe, until the timeframe end point is reached. In case a
repeating range ends later than the containing timeframe, the ending time is
adjusted to match that of the timeframe.

The definition of the starting and ending point of a time range is done in a 
syntax reminisent of the `cron <http://en.wikipedia.org/wiki/Cron>`_ format. 

.. code-block:: yaml

  applicable:
    from:                            # Milliseconds since the epoch
    to:                              # [opt] Milliseconds since the epoch
    repeat:                          # [opt] Defines a repetion list
      - every:                       # [opt] A repetion entry 
        start: "min hr dom moy dow"  # 5-elem cron string
        end:   "min hr dom moy dow"  # 5-elem cron string 

The following declaration defines a timeframe starting at the designated
timestamp and ending at the time of evaluation.

.. code-block:: yaml

  applicable:
    from: 1293703200  #(30/12/2010 10:00)

The following declaration defines a timeframe of one year, within which the
applicability of the specified policy, agreement or pricelist is constrained to
time ranges from 12:00 Mon to 14:00 Fri  (first ``every`` definition)
and 15:00 Sat to 15:00 Sun.

.. code-block:: yaml

  applicable:
    from: 1293703200  #(30/12/2010 10:00)
    to:   1325239200  #(30/12/2011 10:00)
    repeat:
      - every:
        start: "00 12 * * Mon"
        end:   "00 14 * * Fri"
      - every:
        start: "00 15 * * Sat"
        end:   "00 15 * * Sun"

Resources
^^^^^^^^^

A resource represents an entity that can be charged for.

The DSL does not assume a fixed set of resource types and is extensible to any
number of resources. The default set of resources that the DSL supports 
are the following: 

- ``vmtime``: Time a specific VM is operating
- ``diskspace``: Space on disk being used for storing data
- ``bandwidthup``: Bandwidth used for uploading data
- ``bandwidthdown``: Bandwidth used for downloading data

Price lists
^^^^^^^^^^^

A price list defines the prices applicable for a resource within a validity
period. Prices are attached to resource types and denote the policies that
should be deducted from an entity's wallet in response to the entity's resource
usage within a given charging period (currently, a month). The format is the
following:

.. code-block:: yaml

  pricelist:                  # Pricelist structure definition  
    name: apricelist          # Name for the price list, no spaces, must be unique
    [extends: anotherpl]      # [Optional] Inheritance operation: all optional fields  
                              # are inherited from the named pricelist
    bandwidthup:              # Price for used upstream bandwidth per MB 
    bandwidthdown:            # Price for used downstream bandwidth per MB
    vmtime:                   # Price for time 
    diskspace:                # Price for used diskspace, per MB
    applicable:
      [see Timeframe format]

Algorithms
^^^^^^^^^^

An algorithm specifies the algorithm used to perform the cost calculation, by
combining the reported resource usage with the applicable pricelist. As opposed
to price lists, policies define behaviours (algorithms), which have certain
validity periods. Algorithms can either be defined inline or referenced from
the list of defined algorithms. 

.. code-block:: yaml

  algorithm:
    name: default
    bandwidthup:   {price} times {volume} 
    bandwidthdown: {price} times {volume}
    vmtime: {price} times {volume}
    diskspace: {price} times {volume}
    applicable: 
      [see Timeframe format]


Credit Plans
^^^^^^^^^^^^



Agreements
^^^^^^^^^^

An agreement is the result of combining a policy with a pricelist. As the
accounting DSL's main purpose is to facilitate the construction of agreements
(which are then associated to entities), the agreement is the centerpiece of
the language. An agreement is defined in full using the following template:

.. code-block:: yaml

  agreement:
    name: someuniqname        # Unique name for 
    extends: other            # [opt] name of inhereted agreement 
    pricelist: plname         # Name of declared pricelist
      resourse: value         # [opt] Overiding of price for resource
    policy: polname           # Name of declared policy
      resourse: value         # [opt] Overiding of algorithm for resourse

**Consistency requirements:**

- If a ``pricelist`` or ``policy`` name has not been specified, all prices or
  algorithms for the declared resources must be defined in either the processed 
  ``agreement`` or a parent ``agreement``.

The charging algorithm
^^^^^^^^^^^^^^^^^^^^^^



Examples
^^^^^^^^^
.. toctree::

  unicase 


Document Revisions
------------------

==================    ================================
Revision              Description
==================    ================================
0.1 (Nov 2, 2011)     Initial release. Credit and debit policy descriptions 
0.2 (Feb 23, 2012)    Update definitions, remove company use case
==================    ================================


