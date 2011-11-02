Aquarium Development Guide
==========================

Introduction
------------

The development guide includes descriptions of the APIs and extention points
offered by Aquarium. It also includes design and development setup information.

Document Revisions
^^^^^^^^^^^^^^^^^^

==================    ================================
Revision              Description
==================    ================================
0.1 (Nov 2, 2011)     Initial release. Credit and debit policy description and use cases
==================    ================================

The accounting DSL
------------------

The Aquarium accounting policy DSL enables administrators to specify accounting
processing policies, price lists and combine them arbitrarily into agreements
applicable to specific users or the whole system. The DSL's primary purpose is
to facilitate the definition of agreements through defining and composing cost
calculation policies with price lists.

As the agreements mostly consist of data, while behavior definitions are quite
limited , the DSL is mostly based on the
"YAML":http://en.wikipedia.org/wiki/Yaml format. The DSL supports limited
algorithm definitions through a simple imperative language as defined below.

The DSL supports inheritance for policies, price lists and agreements and composition in the case of agreements.

Resources
^^^^^^^^^

The DSL does not assume a fixed set of resource types and is extensible to any
number of resources. Currently, the default set of resources that the DSL
supports are the following: 

- ``vmtime``: Time a specific VM is operating
- ``diskspace``: Space on disk being used for storing data
- ``bandwidthup``: Bandwidth used for uploading data
- ``bandwidthdown``: Bandwidth used for downloading data

The agreement model
^^^^^^^^^^^^^^^^^^^

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

Price lists
^^^^^^^^^^^

A price list defines the prices applicable within a validity period. Prices are
attached to resource types and denote the credits that should be deducted from
an entity's wallet in response to the entity's resource usage within a given
charging period (currently, a month). The format is the following:

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



Policies
^^^^^^^^

A policy specifies the algorithm used to perform the cost calculation, by
combining the reported resource usage with the applicable pricelist. As opposed
to price lists, policies define behaviours (algorithms), which have certain
validity periods. Algorithms can either be defined inline or referenced from
the list of defined algorithms. 

.. code-block:: yaml

  policy:
    name: default
    bandwidthup:   {price} times {volume} 
    bandwidthdown: {price} times {volume}
    vmtime: {price} times {volume}
    diskspace: {price} times {volume}
    applicable: 
      [see Timeframe format]

Implicit variables
^^^^^^^^^^^^^^^^^^

Implicit variables are placeholders that are assigned a value at evaluation
time. Variables are always bound to a resource declaration within a policy.
The following implicit values are supported:

- ``price``: Denotes the price for the designated resource in the applicable agreement
- ``volume``: Denotes the runtime usage of the designated resource

Operators
^^^^^^^^^

- Conditionals: 
  - @if...then...elsif...else...end@ Conditional decisions. 

- Comparison:
  - @gt, lt@: @>@ and @<@

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
syntax reminisent of the "cron":http://en.wikipedia.org/wiki/Cron format. 

.. code-block:: yaml

  applicable:
    from:
    [to]:
    [repeat]:
      - every:
        start: "sec min hr dow moy"
        end:   "sec min hr dow moy"

The following declaration defines a timeframe starting at the designated
timestamp and ending at the time of evaluation.

.. code-block:: yaml

  applicable:
    from: 1293703200  #(30/12/2010 10:00)


The following declaration defines a timeframe of one year, within which the
applicability of the specified policy, agreement or pricelist is constrained to
time ranges from 12:00 Mon to 14:00 Fri  (first <code>every</code> definition)
and 15:00 Sat to 15:00 Sun.

.. code-block:: yaml

  applicable:
    from: 1293703200  #(30/12/2010 10:00)
    to:   1325239200  #(30/12/2011 10:00)
    repeat:
      - every:
        start: "00 00 12 1 *"
        end:   "00 00 14 5 *"
      - every:
        start: "00 00 15 Sat *"
        end:   "00 00 15 Sun *"

