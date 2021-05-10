# Installation and Configuration Guide
Flowgate can be installed by either of the follow approaches: 

- **Install from Source code:** Build and install Flowgate from source code. Please refer to **[Flowgate Compile Guide](compile_guide.md)**.

- **Install from Binary:** Download prebuild binary from **[official release](https://github.com/vmware/flowgate/releases)** page.


This guide describes the steps to install and configure Flowgate by using the Binary installer.

## Prerequisites for the target host
Flowgate is deployed as several Docker containers, and can be deployed on any Linux distribution that supports Docker. The target host requires Docker, and Docker Compose to be installed.  
### Hardware
|Resource|Capacity|Description|
|---|---|---|
|CPU|minimal 2 CPU|4 CPU is preferred|
|Mem|minimal 8GB|16GB is preferred|
|Storerage|minimal 80GB|160GB is preferred|
### Software
|Software|Version|Description|
|---|---|---|
|Docker engine|version 1.10 or higher|For installation instructions, please refer to: https://docs.docker.com/engine/installation/|
|Docker Compose|version 1.6.0 or higher|For installation instructions, please refer to: https://docs.docker.com/compose/install/|
### Network ports 
|Port|Protocol|Description|
|---|---|---|
|443|HTTPS|Flowgate portal and core API will accept requests on this port for https protocol|

## Installation Steps

### Binary Installation

1. Download the installer "flowgate-v1.1-offline-installer.tar.gz" from **[official release](https://github.com/vmware/flowgate/releases)** pages.
2. unzip the binary package.
```
    $ tar -zxvf flowgate-*.tar.gz
```

3. Initialize Flowgate

```
    $ cd flowgate
    $ sudo bash flowgate_init.sh
    ...
    conf/management/
    conf/management/application.properties
    conf/aggregator/
    conf/aggregator/application.properties
    Creating flowgate-database-container
    Stopping flowgate-database-container ... done
    Removing flowgate-database-container ... done
    Removing network mavendockerbuild_db-network
    Removing network mavendockerbuild_services-network
    Flowgate Initialize Success.
```

4. Start Flowgate

```
    $ sudo bash flowgate_run.sh
    ...
    Creating network "mavendockerbuild_db-network" with the default driver
    Creating network "mavendockerbuild_services-network" with the default driver
    Creating flowgate-database-container
    Creating flowgate-redis-container
    Creating flowgate-api-container
    Creating flowgate-vc-worker-container
    Creating flowgate-nlyte-worker-container
    Creating flowgate-poweriq-worker-container
    Creating flowgate-aggregator-container
    Creating flowgate-management-container
    Creating flowgate-labsdb-worker-container
    Creating flowgate-vro-worker-container
    Creating flowgate-infoblox-worker-container

```
*Note*:
1. Source Installation: the flowgate_run.sh and flowgate_init.sh under *flowgate/make/*.
2. Binary Installation: the flowgate_run.sh and flowgate_init.sh extract from flowgate-*.tar.gz.

## Start Using

If everything worked properly, you should be able to open a browser to visit the admin portal at **https://yourdomain** (change *yourdomain* to your server's hostname or ip). Note that the default administrator username/password are admin/Admin!23. Please change it immediately.

### Managing Flowgate's lifecycle
You can use docker-compose to manage the lifecycle of Flowgate. Some useful commands are listed as follows (must run in the same directory as *docker-compose.run.images.yml*).

Stopping Flowgate:
```
$ sudo docker-compose -f docker-compose.run.images.yml down
Stopping flowgate-labsdb-worker-container ... done
Stopping flowgate-nlyte-worker-container ... done
Stopping flowgate-vc-worker-container ... done
...
Removing flowgate-api-container ... done
Removing flowgate-redis-container ... done
Removing flowgate-database-container ... done
Removing network mavendockerbuild_db-network
Removing network mavendockerbuild_services-network
```  
Restarting Flowgate after stopping:
```
$ sudo docker-compose -f docker-compose.run.images.yml up -d
Creating network "mavendockerbuild_db-network" with the default driver
Creating network "mavendockerbuild_services-network" with the default driver
Creating flowgate-database-container
Creating flowgate-redis-container
Creating flowgate-api-container
Creating flowgate-aggregator-container
Creating flowgate-nlyte-worker-container
Creating flowgate-labsdb-worker-container
Creating flowgate-management-container
Creating flowgate-vc-worker-container
Creating flowgate-infoblox-worker-container
Creating flowgate-vro-worker-container
Creating flowgate-poweriq-worker-container
```  

## Troubleshooting
1. When Flowgate does not work properly, check the Flowgate services status by: 
```
    $ sudo docker-compose -f docker-compose.run.images.yml ps
               Name                             Command               State                    Ports                  
---------------------------------------------------------------------------------------------------------------------
flowgate-aggregator-container        sh start.sh                      Up                                              
flowgate-api-container               sh start.sh                      Up      49610/tcp                               
flowgate-infoblox-worker-container   sh start.sh                      Up                                              
flowgate-labsdb-worker-container     sh start.sh                      Up                                              
flowgate-management-container        sh start.sh                      Up      443/tcp, 0.0.0.0:443->49611/tcp, 80/tcp 
flowgate-database-container          docker-entrypoint.sh             Up      11207/tcp, 11210/tcp, 11211/tcp,...                             
flowgate-nlyte-worker-container      sh start.sh                      Up                                              
flowgate-poweriq-worker-container    sh start.sh                      Up                                              
flowgate-redis-container             docker-entrypoint.sh redis ...   Up      6379/tcp                                
flowgate-vc-worker-container         sh start.sh                      Up                                              
flowgate-vro-worker-container        sh start.sh                      Up  
```
If a container is not in **Up** state, check the log file of that container in directory ```/opt/vmware/flowgate/log```. For example, if the container ```flowgate-api-container``` is not running, you should look at the log file ```/opt/vmware/flowgate/log/flowgate-api/*.log```.  
