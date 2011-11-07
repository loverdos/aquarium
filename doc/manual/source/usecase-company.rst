Use cases for a company
====

Here we present use cases related to the context of a company and mainly from the credits point of view.
We show how several company units and individuals use the
cloud infrastructure provided by GRNET, either in its entirety or partially. For reference, we use the term Okeanos to
indicate the respective cloud offering. For cases where credit distribution details must be specified, we use a respective
DSL (Domain Specific Language) in YAML format. Thus, we blend high-level descriptions with a technical representation.
The exact semantics of this DSL are to be defined elsewhere. The DSLs shown are not part of the system design but may guide us
to it.

Each use case tries to demonstrate a minimal and self-contained example. This means that more complex use uses can
be easily constructed by merging two or more of the given ones.

The list of use cases is not meant to be exhaustive.

Glossary of Entities
----

- *Employee*: An individual that works for the company.
- *Organizational Unit*: An organizational structure within the company which has a specific function.
  Organizational units can be further analyzed into sub-units.
- *General Directorate*: The top-level organizational unit within a company.
- *Directorate*: An organizational unit that is directly under the supervision of a General Directorate.
- *Section*: An organizational unit that is directly under the supervision of a Directorate.
- *Director*: An employee who manages a business unit.
- *Okeanos*: The cloud offering by GRNET. Depending on context, this may be either the full cloud stack or parts of it,
  e.g. Aquarium only.
- *Aquarium*: The part of GRNET's cloud offering that deals with credits, accounting, billing, resource sharing.
- *Credit structure*: A composite entity that can accumulate credits and can distribute them to underlying members.
- *Credit strucure level* or *Credit level*: When creating hierarchies of credit structures, the credit (structure) level
  is the hierarchy level. For example, the top level credit structure has a level of zero (0) or one (1) depending on
  context (and basically whichever suits best the particular case).
- *Credit holder*: An entity that owns credits. Can be a person or a composite credit structure.
- *Parent credit structure*: In a hierarchical credit structure setting, for a given credit structure *A*,
  its parent is the credit structure that has *A* as a member.


Organizational structure
----
We introduce the following organizational structure of the company:

- There are two *General Directorates*:

  - Technical General Directorate. It is made of the following *Directorates*:

    - Operations Directorate
    - Network Directorate
    - Technology Directorate. This Directorate is further made of the following *Sections*:

      - Software Architecture Section
      - Reasearch and Development (R&D) Section
      - Middleware Section

  - Business General Directorate. It is made of the following Directorates:

    - Business Strategy & Development Directorate
    - Sales Directorate


Each General Directorate is headed by a *General Director* and each Directorate is headed by a *Director*. A Section is
headed by a *Section Manager*.


Infrastructure
----

Cloud mode Provisioning
^^^^

Case
++++

Okeanos is provided externally.

The company has outsourced all of its infrastructure to an external Okeanos/cloud provider. All infrastructure support and
operation tasks are performed by the external cloud provider. The initial administrative authority on behalf of the
company is created, on request, by the cloud provider. All other users and credit structures are created by the Company
itself.


Hosted mode Provisioning
^^^^

Case
++++

Okeanos is provided in-house.

The company has its own installation of Okeanos and is its administrator. All infrastructure support and operation tasks
are performed by properly trained staff. The creation of single users and composite credit structures is performed by
Company staff.



Credit structure modelling
----


Follow organization structure
^^^^

Case
++++

The company credit structure is modelled after the organization structure.

Every organizational unit corresponds to a credit structure. The members of each credit structure are taken
from the member list of the respective organizational unit. So, we have the following credit structure:

  - Technical. It is made of the following (sub)structures:

    - Operations.
    - Network.
    - Technology. This structure is further made of the following structures:

      - Software Architecture
      - Reasearch and Development (R&D)
      - Middleware

  - Business. It is made of the following structure:

    - Business Strategy & Development

Comparing the above to the organizational structure, we see that there is no *Sales* structure. So the mapping from an
organization structure to a credit structure is optional: the only requirement in this use case is that when the mapping
exists it should be exact.


Credit DSL
++++

.. code-block:: yaml

  credit-structure:
    name: Technical
    label: Technical # A unique-per-company label, no spaces, no quotes
    owner: user:Technical_General_Director_Alias # A URI for the general director
    members:
      - Operations
      - Network
      - Technology

  credit-structure:
    name: Business
    label: Business
    owner: user:Business_General_Director_Alias
    members:
      - Business_Strategy_and_Development

  credit-structure:
    name: Technology
    label: Technology
    owner: user:Technology_Director_Alias # A URI for the director
    members:
      - Software_Architecture
      - Research_and_Development
      - Middleware

  credit-structure:
    name: "Business Strategy & Development"
    label: Business_Strategy_and_Development
    owner: user:Business_Strategy_and_Development_Director_Alias # A URI for the director
    members:
      - employee:1234
      - employee:1235
      - employee:1236

Do not follow organization structure
^^^^

Case
++++
The company credit structure does not follow the organization structure.

The company credit structure has (rather historically) been modelled on demand as follows:

  - IT cloud
    All the infrastructure belongs to this credit structure

    - Production cloud
      This is used to power the company's business in the outside world

    - Development and testing cloud
      The infrastructure used to develop and test new products and services.

    - R&D cloud
      This is special credit structure that is used for technical and business R&D

    - Data Warehouse cloud
      Infrastructure that is used for loyalty campaigns, analytics, market research and reports.


Note how the credit structure names have a "cloud" suffix to reflect their computational nature.

Credit DSL
++++

.. code-block:: yaml

  credit-structure:
    name: "IT cloud"
    label: IT_cloud # A unique-per-company label
    owner: user:TechnicalGeneralDirectorAlias # A URI for the general director
    members:
      - Production_cloud
      - Development_and_testing_cloud
      - RnD_cloud
      - Data_Warehouse_cloud



Credit distribution between credit holders
----

Strict hierarchical credit distribution
^^^^

Case
++++

Credits are distributed from a credit structure level to immediately lower credit levels.

For example, based on the company-wide infrastructure planning strategy, each year the General Director assigns credits to the
Techical and Business credit structures. Their respective Directors appropriately distributed the credits they receive
from the Director to their underlying structures and so on. In case the demand on resources exceeds the original planning, the
procedure of top-down credit distribution can be re-initiated at will by the General Director. Credit usage can become part
of the company KPIs (key performance indicators).


Credit DSL
++++

Notice how this is the same as in the case of 'Follow organizational structure' shown previously.

.. code-block:: yaml

  credit-structure:
    name: Technical
    label: Technical # A unique-per-company label, no spaces, no quotes
    owner: user:Technical_General_Director_Alias # A URI for the general director
    members:
      - Operations
      - Network
      - Technology

  credit-structure:
    name: Business
    label: Business
    owner: user:Business_General_Director_Alias
    members:
      - Business_Strategy_and_Development

  credit-structure:
    name: Technology
    label: Technology
    owner: user:Technology_Director_Alias # A URI for the director
    members:
      - Software_Architecture
      - Research_and_Development
      - Middleware

  credit-structure:
    name: "Business Strategy & Development"
    label: Business_Strategy_and_Development
    owner: user:Business_Strategy_and_Development_Director_Alias # A URI for the director
    members:
      - employee:1234
      - employee:1235
      - employee:1236


Relaxed hierarchical credit distribution
^^^^

Case
++++

Credits are distributed from a credit structure level to any lower credit levels.

For this use case, we assume that the credit structure follows the company organizational structure, as explained
previously. Then, the General Director can give credits to the the R&D credit structure possibly because he/she has
directly assign them a specific R&D task.


Credit DSL
++++
We can extend the concept of credit structure hierarchy to that of credit structure DAG (Directed Acyclic Graph). In this
example, under the Technical credit structure (which corresponds to a General Directorate) we not only classify
Operations, Network and Technology (which correspond to Directorates) but also Research and Development (which corresponds
to a Section).

.. code-block:: yaml

  credit-structure:
    name: Technical
    label: Technical # A unique-per-company label, no spaces, no quotes
    owner: user:Technical_General_Director_Alias # A URI for the general director
    members:
      - Operations
      - Network
      - Technology
      - Research_and_Development # This is under Technology as well

  credit-structure:
    name: Business
    label: Business
    owner: user:Business_General_Director_Alias
    members:
      - Business_Strategy_and_Development

  credit-structure:
    name: Technology
    label: Technology
    owner: user:Technology_Director_Alias # A URI for the director
    members:
      - Software_Architecture
      - Research_and_Development
      - Middleware

  credit-structure:
    name: "Business Strategy & Development"
    label: Business_Strategy_and_Development
    owner: user:Business_Strategy_and_Development_Director_Alias # A URI for the director
    members:
      - employee:1234
      - employee:1235
      - employee:1236


Strict P2P credit distribution
^^^^

Case
++++
Credits can be distributed in a P2P fashion between credit holders (peers) of the same credit level.


Peers are either employees or credit structures at the same level of credit structure organization. For example, under
*Technology* structure, the *Software Architecture* and *R&D* structures can move credits between each other's wallet. Also,
*Employee Alpha* of R&D and *Employee Beta*, also of R&D, can distribute credits to one another.

Note that *strict* means that the credit holders are at the same credit level. As shown by a previous use case, this can
be or not the same as the organizational level.



Relaxed P2P credit distribution
^^^^

Case
++++
Credits can be distributed in a P2P fashion between credit holders (peers) regardless of their credit level.

Two or more employees can distribute credits to one another, regardless of the credit structure they belong to. Also,
two or more credit structures can distribute credits to one another, regardless of their parent credit structure.

Credit DSL
++++
Here, ``credit-distribution`` denotes some form of authorization to distribute credits. This kind of authorization
was implicit in the previous DSL examples where ``credit-structure`` was defined, since by definition a ``credit-structure`` is
created in order to distribute credits.

.. code-block:: yaml

  credit-distribution:
    source: employee:1234 # Employee Alpha
    target: employee:9256 # Employee Beta

  credit-distribution:
    source: employee:9256 # Employee Beta
    target: employee:1234 # Employee Alpha

  credit-distribution:
    source: structure:1 # Sofware Architecture
    target: structure:2 # R&D



Free-form credit distribution
^^^^

Case
++++
Credits can be distributed from a credit holder to any other credit holder.


There are no structural constraints in credit distribution. Any credit holder has the ability to manage and distribute the
respective credits at will.

For example, two company employees, namely *Employee Alpha* and *Employee Beta*, are given the task to investigate some R&D scenario. Management
fills their respective personal credit wallets with a credit amount that they are free to use at will in order to
fulfill their task. The two employees quickly setup a credit structure named *R&D Lab Rho* to which they give a percentage
of their credits. Each one is free to spend their remaining personal credits at will but at some point, Employee Alpha
has an empty credit wallet and requests a few credits from Employee Beta, who agrees to provide them. At a later time,
management decides it would be advantageous to engage a business user, *Employee Omega*, from a different part of the company
as an external sponsor and observer.
Employee Omega is also given credits to help the other two employees but does not actually join R&D Lab Rho. Employee Omega
is an external observer who consults on the results and contributes credits to either the two other employees or the R&D Lab Rho
explicitly.


Credit DSL
++++

Here, the ``credit-structure`` definition implicitly describes a credit flow from the structure to the underlying
employees, while the ``credit-distribution`` definitions describe credit flow for other cases.

.. code-block:: yaml

  credit-structure:
    name: "R&D Alpha Rho"
    label: RnD_Alpha_Rho
    members: # Note how Employee Omega is not part of the credit structure
      - employee:1234 # Employee Alpha
      - employee:9256 # Employee Beta

  credit-distribution:
    source: employee:1234 # Employee Alpha
    targets:
      - employee:9256 # Employee Beta
      - structure:3 # RnD_Alpha_Rho

  credit-distribution:
    source: employee:9256 # Employee Beta
    targets:
      - employee:1234 # Employee Alpha
      - structure:3 # RnD_Alpha_Rho

  credit-distribution:
    source: employee:8888 # Employee Omega
    targets:
      - employee:1234 # Employee Alpha
      - employee:9256 # Employee Beta
      - structure:3 # RnD_Alpha_Rho



Credit distribution policies: when & how
----

Assuming a credit structure with its members, the question is when and how credits are distributed from the parent
credit holder to the member holders. The same ideas can of course be considered for distribution between peers.

Periodic, algorithmic credit distribution to members
^^^^

Case
++++
Credits are distributed to credit structure members in a periodic fashion and based on a particular algorithm.

Given a particular credit structure, a system process (provided and administered by the cloud infrastructure) runs
periodically and distributes the credits own by the structure to its members, according to an agreed upon and pre-specified
algorithm. For example, the administrator of the R&D structure decides that only eighty percent (80%) of the structure
credits are distributed equally to each one of the members. In this case, the algorithm is represented by the formula

  0.80 * credits / N

where N is the number of the members.

In the above description, we have assumed that the credit structure has its own credits.

Credit DSL
++++

.. code-block:: yaml

  credit_structure:
    name: "Research & Development (R&D)"
    label: Research_and_Development
    members:
      - employee:1234
      - employee:1235
      - employee:1236
    credit_policy:
      when: Periodic
        period: 1 month
      how: Agorithmic
        formula: 0.80 * $credits / $member_count


Manual, equal amount credit distribution to members
^^^^

Case
++++

Credit DSL
++++