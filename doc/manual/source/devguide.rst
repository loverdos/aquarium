Aquarium Development Guide
==========================

The development guide includes descriptions of the APIs and extention points
offered by Aquarium. It also includes design and development setup information.

The accounting system
----------------------

The accounting subsystem deals with 
accepts usage events from other systems and translates
them to accounting entries in the user's wallet. 

Entities
^^^^^^^^

- *Raw Event*: A raw event is generated from an external source and are permanently appended in an immutable event log. A raw event carries information about changes in an external system that affect the status of a user's wallet. Raw events processed by the system are listed below.
- *AccountingEvent*: An accounting event is the result of processing one or more raw events, and is the only input to the accounting system. An accounting event associates the
- *AccountingEntry*: An accounting entry is the result of processing one accounting event and is what gets stored to the user's wallet.
- *Resource*: A resource represents an entity that can be charged for its usage. The currently charged resources are: Time of VM usage, bytes uploaded and downloaded and bytes used for storage
- *PriceList*: A price list contains information of the cost of a resource. A specific pricelist is only applied within a specified time frame.
- *Policy*: A policy specifies the way the charge calculation is done. It can be vary depending on resource usage, time of raw event or other information.
- *Agreement*: An agreement associates pricelists with policies and users.


Common syntax
^^^^^^^^^^^^^

Implicit variables
~~~~~~~~~~~~~~~~~~

Implicit variables are placeholders that are assigned a value at evaluation
time. Variables are always bound to a resource declaration within a policy.
The following implicit values are supported:

- ``price``: Denotes the price for the designated resource in the applicable agreement
- ``volume``: Denotes the runtime usage of the designated resource

Operators
~~~~~~~~~

- Conditionals: ``if...then...elsif...else...end`` Conditional decisions. 
- Comparison: ``gt, lt``: ``>`` and ``<``

Time frames
~~~~~~~~~~~

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


.. toctree::

  debitdsl 
  creditdsl


Document Revisions
^^^^^^^^^^^^^^^^^^

==================    ================================
Revision              Description
==================    ================================
0.1 (Nov 2, 2011)     Initial release. Credit and debit policy descriptions 
==================    ================================


