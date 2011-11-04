A typical university
^^^^^^^^^^^^^^^^^^^^

A university wants to setup a cloud for use by their Network Operation Services,
in order to streamline machine provisioning operations for both the NOC's needs
and university department, laboratory and individual persons requests. The 
NOC currently uses a rigid resource distribution policy, where all resource provisioning requests go through the approval of NOC officials. 

As most universities, the 

The cloud services that the university NOC can offer are the following:

- Virtual machines of three different configurations (hereby: A, B, C)
- Volumes for virtual machines
- File storage service, for storing files
- Custom virtual private networks


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
          start: 0
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
          start: 0
    agreements:
      agreement:
        name: default
        policy: default
        pricelist: default

