## Upgrade Guide From v1.1.2 To v1.2.1

1. Download the installer "flowgate-v1.2.0-offline-installer.tar.gz" from **[official release](https://github.com/vmware/flowgate/releases)** pages and upload it to your flowgate server
2. Unzip the binary package
	```shell
	$ tar -zxvf flowgate-v1.2.1-offline-installer.tar.gz
    ```
3. Stop your flowagte service
    ```shell
    $ systemctl stop flowgate
    ```
4. Backup your old data and conf
    ```shell
    tar -cvf backup.tar /opt/vmware/flowgate/conf/ /opt/vmware/flowgate/data/ /opt/vmware/flowgate/docker-compose.run.images.yml
    ```
5. Run upgrade.sh
	```shell
	$ cd flowgate/script/1.2.1
	$ ./upgrade.sh
	```
	If everything works properly, you can get the below message: 
	``` 
	...
	Step 4/4 : Restarting flowgate
	Flowgate started
	```
