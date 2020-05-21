## Introduction

This guide provides instructions for developers to build Flowgate from source code.

### Step 1: Prepare for a build environment for Flowgate

Flowgate is deployed as several Docker containers and most of the code is written in Java language. The build environment requires Docker, Docker Compose. Please install the below prerequisites:

Software              | Required Version
----------------------|--------------------------
docker                | 1.10 +
docker-compose        | 1.6.0 +
git                   | latest is preferred

### Step 2: Getting the source code

   ```
      $ git clone git@github.com:vmware/flowgate.git
   ```

### Step 3: Building Flowgate

*  Build Flowgate:

   ```
      $ cd flowgate/make/
      $ sudo bash build.sh all -version v1.1
   ```

### Step 4: Verify build

If everything works properly, you can get the below message:

   ```
      ...
      save docker images...
      flowgate_run.sh
      conf.tar.gz
      maven-docker-build/docker-compose.run.images.yml
      docker-images-output/flowgate.tar
      build success.
   ```
Also, you can execute below command to verify the container images:

   ```
      $ sudo docker images
      REPOSITORY                 TAG                 IMAGE ID            CREATED             SIZE
      flowgate/labsdb-worker     v1.1                b751b1e55ec9        5 minutes ago         228MB
      flowgate/nlyte-worker      v1.1                4c1b5c0c1893        5 minutes ago         226MB
      flowgate/aggregator        v1.1                a6ada87420a8        5 minutes ago         229MB
      flowgate/poweriq-worker    v1.1                b1d6997bd8c8        5 minutes ago         226MB
      flowgate/vro-worker        v1.1                6910c721130c        5 minutes ago         231MB
      flowgate/vc-worker         v1.1                81c255461862        5 minutes ago         255MB
      flowgate/redis             v1.1                b0d6035edaf6        5 minutes ago         102MB
      flowgate/api               v1.1                0b71d1eea75b        5 minutes ago         238MB
      flowgate/database          v1.1                a56dcb8f1dad        5 minutes ago         271MB
      flowgate/infoblox-worker   v1.1                227b8304775d        5 minutes ago         221MB
      flowgate/management        v1.1                939197e11efe        5 minutes ago         249MB
   ```
## Start Flowgate

1. Initialize Flowgate

```
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
2. Start Flowgate

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
## Start Using
**[Click here to start](https://github.com/vmware/flowgate/blob/master/docs/installation_guide.md#Start-Using)**
## Appendix
* Using the build.sh

The `build.sh` contains these configurable parameters:

Variable           | Description
-------------------|-------------
ui                 | only build ui.
jar                | build jar package. (You must execute the above orders.)
image              | build all docker images. (You must execute the above orders.)
save               | save all of the docker images to a tar. (You must execute the above orders.)
all                | build all above steps.
version            | Specify a version number for Flowgate.

#### EXAMPLE:

#### execute build script

   ```sh
      $ sudo bash build.sh
      eg. 'bash build.sh ( ui | jar | image | save | all ) -version v1.1'
   ```
## Troubleshooting
1. When Flowgate build timeout:
```
   error. timeout. maybe build jar fault
```
you can check build log at ```flowgate/make/build-log/flowgate-build-error-log.txt```
