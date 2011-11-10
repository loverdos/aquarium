A typical university
^^^^^^^^^^^^^^^^^^^^

A university wants to setup a cloud for use by their Network Operation Services,
in order to streamline machine provisioning operations for both the NOC's needs
and university department, laboratory and individual persons requests.

The university under examination is organized as a typical high education
organisation: It has schools, which are further split in departments. Each
department has students and professors. Professors can start independent
research units (laboratories) which also have students but do not fall under
the auspices of either schools or departments.

The NOC currently uses a rigid resource distribution policy, where all resource
provisioning requests have to go through the approval of NOC officials. This
means that the resource sharing hierarchy is flat, while all credit allocation
policies are negotiated directly with the NOC. 

The cloud services that the university NOC can offer are the following:

- Virtual machines of three different configurations (hereby: A, B, C)
- Volumes for virtual machines
- File storage service, for storing files
- Custom virtual private networks

After installing Aquarium and configuring the external services to talk to it,
the NOC configures administrator users and specifies the following policy as
the initial base policy for all users and services. All the remaining
organizational entities (departments, students etc) are created on request on
service initialization.

.. code-block:: yaml

  creditdsl:
    resources:
      - vmtimeA
      - vmtimeB
      - vmtimeC
      - volumedisk
      - filedisk
      - netbandwidth
    pricelists:
      pricelist: 
        name: default
        vmtimeA: 1
        vmtimeB: 1.5
        vmtimeC: 2
        volumedisk: 0.1
        filedisk: 0.2
        netbandwidth: 0.01
        applicable: 
          from: 0
    policies:
      policy:
        name: default
        vmtimeA: {price} times {volume}
        vmtimeB: {price} times {volume}
        vmtimeC: {price} times {volume}
        volumedisk: {price} times {volume}
        filedisk: {price} times {volume}
        netbandwidth: {price} times {volume}
        applicable: 
          from: 0
    agreements:
      agreement:
        name: default
        policy: default
        pricelist: default


Student user wants a new VM
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The simplest request a NOC has to deal with is the request by a student
to create a new virtual machine. The student will be assigned to the 
default service agreement, which provides her with a fix number of credits
and the base charging plan. 

All the NOC that has to do in this case is to import to Aquarium the user's
account in the identity provision mechanism, and allowing it access to 
the VM service.

New research team is founded 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Prof X creates a new research team, which will need a multitude of
infrastructure services, such as shared storage space, several VMs of
various configurations for things like web servers and experiment testbeds
and a dedicated VPN used by all team members to connect to team servers remotely.
All resources own by the team are charged to the team. The team's focus will
be data warehousing, which means large volumes of raw data shared by virtual
clusters of machines.

Given the team's anticipated space requirements and in order to motivate team
members to clean up intermediate results, the NOC administration negotiates an
increased price for storage resources and a lowered price for CPU bound tasks.
Moreover, to promote a round the clock usage of resources it (i.e. to make the
team members run experiments at night and on the weekends) the NOC offers a
discount for those time periods on all resources. The NOC creates the following
charging policy:

.. code-block:: yaml

  agreement:
    name: forteamx
    pricelist:
      vmtimeA: 0.5
      volumedisk: 0.2
      filedisk: 0.25
      applicable: 
        from: 1320665415 #7/11/2011 13:30
    policy:
      volumedisk: {price} * 0.7
      filedisk: {price} * 0.7
      applicable:
        from: 1320665415 #7/11/2011 13:30
        repeat:
          - every:
            start: "00 00 * * Mon-Fri"
            end:   "00 07 * * Mon-Fri"
          - every:
            start: "00 00 * * Sat"
            end:   "59 23 * * Sun"

The team is defined as a group and all individual team members are added to it.
[ckkl]

New price plan and resource
~~~~~~~~~~~~~~~~~~~~~~~~~~~

After some time the service has been active by the NOC, an upgrade to the
hardware capacity urges the NOC administration to set a lower price to the
default price plan. Moreover, since the new capacity allows it, a new VM
configuration is added to the set of offered resources. The above mean that
all agreements that inherit from the default will need to be recursively
updated while agreement history will need to be preserved.

To cope with the above, the NOC administrators add a new default policy that
`supersedes` the old one and set the applicability period of the old default 
pricelist to end at the point where the new pricelist comes in effect. 

.. code-block:: yaml

  creditdsl:
    resources:
      - vmtimeA
      - vmtimeB
      - vmtimeC
      - vmtimeD
      - volumedisk
      - filedisk
      - netbandwidth
    pricelists:
      pricelist: 
        name: olddefault
        vmtimeA: 1
        vmtimeB: 1.5
        vmtimeC: 2
        volumedisk: 0.1
        filedisk: 0.2
        netbandwidth: 0.01
        applicable: 
          from: 0
          to: 1320665415 #7/11/2011 13:30
      pricelist: 
        name: default
        superseeds: olddefault
        vmtimeA: 0.5
        vmtimeB: 1
        vmtimeC: 1.5
        vmtimeD: 2
   
