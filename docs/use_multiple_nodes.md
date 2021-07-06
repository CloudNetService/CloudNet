# Using more as one node in CloudNet

The instances of CloudNet, also called Nodes, can be easily connected.  
This allows tasks, groups, and local templates to synchronize with the network and distribute   
the services to each node. With appropriate configuration, this can significantly increase the   
overall resilience of the network. It also allows easy task distribution.

Each cluster has its own clusterId, which can be found in config.json. It is important that all nodes in the cluster
have the same "clusterId" in their configuration file.

All nodes in the network must be interconnected. There are no direct master / slave behaviors,   
but only the rule that the node that connects to another node   
gets the information from the already existing node and updates it. (Except the cluster configuration)

### Add a node in the cluster

The addition of a node can either be done at runtime, or within the configuration file.

It is recommended to use the "cluster" command to add a node.

```
cluster add e760a2c3 127.0.0.1 1411
```

or in config.json

```json
  {
     "cluster": {
      "clusterId": "d5e96e6d-ce98-43fc-a33d-a456eeb43561",
      "nodes": [
        {
          "uniqueId": "e760a2c3",
          "listeners": [
            {
              "host": "127.0.0.1",
              "port": 1411
            }
          ],
          "properties": {}
        }
      ]
    }
  }
```

The node with the specified name must not be online at the time, because the latter must then connect,   
and then, as the client of the online node, first retrieve all information, such as tasks, groups, templates,   
node information, ServiceInfoSnapshots. It is important that the initial setup, the node is online, which has the most  
up-to-date information. At runtime, where all nodes are online, in the core system of CloudNet any  
information like the one mentioned above should be in sync.  
All new ones will receive the information and update their own.

### Update manual information

With the command "cluster push" the Local Templates, Tasks and Groups, the bsp.   
via configuration file or template changes have just been changed. Manually updated and synchronized on the network.

```
cluster push templates
cluster push tasks
cluster push groups
```

### Remove a node in cluster

This operation should be done manually. As with the addition, this must be configured for each node, but here in the
config.json.

The JSON object to the node to be removed just needs to be removed. Then should also with the command

```
reload confirm
```

Also the connection to the node will be closed. It is recommended to restart the node.

### How do modules work in the cluster?

Modules do not have the duty to work in a computer network. The default modules of CloudNet are,  
with a few exceptions, designed like the Cloudflare Module for the use of multiple nodes. Important here is that  
these modules are then installed individually on each node. This process is not automated because   
there is a duty with the functionality to work in the cluster. If needed, the appropriate developer,   
such a function, can install it via the API itself. ;)

### How does the API work with multiple nodes?

CloudNet's Driver API is designed to use a single and a cluster. As a result, changes in the  
plugin development are rarely necessary. In module development, most areas of the CloudNet application  
beyond the Driver API are designed to automatically update the data. If necessary, individual areas can be expanded.  
