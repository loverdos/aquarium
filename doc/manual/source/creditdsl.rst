The policy DSL
^^^^^^^^^^^^^^

The policy DSL enables administrators to specify billing algorithms, price lists and combine them arbitrarily into agreements
applicable to specific users or the whole system. The DSL's primary purpose is
to facilitate the definition of agreements through defining and composing cost
calculation policies with price lists.


The DSL supports inheritance for policies, price lists and agreements and composition in the case of agreements.

Resources
~~~~~~~~~

A resource represents an entity that can be charged for.

The DSL does not assume a fixed set of resource types and is extensible to any
number of resources. The default set of resources that the DSL supports 
are the following: 

- ``vmtime``: Time a specific VM is operating
- ``diskspace``: Space on disk being used for storing data
- ``bandwidthup``: Bandwidth used for uploading data
- ``bandwidthdown``: Bandwidth used for downloading data


Price lists
~~~~~~~~~~~

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


Policies
~~~~~~~~

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


Agreements
~~~~~~~~~~

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


