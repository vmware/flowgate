## Upgrade Guide From v1.1.2 To v1.2.0

1. Login your flowgate server and stop your flowagte service
    ```shell
   $ systemctl stop flowgate
    ```
2. Backup your old data and conf
    ```shell
    tar -cvf backup.tar /opt/vmware/flowgate/conf/ /opt/vmware/flowgate/data/ /opt/vmware/flowgate/docker-compose.run.images.yml
    ```
3. Download the installer "flowgate-v1.2.0-offline-installer.tar.gz" from **[official release](https://github.com/vmware/flowgate/releases)** pages and upload it to your flowgate server
4. unzip the binary package
    ```shell
	$ tar -zxvf flowgate-v1.2.0-offline-installer.tar.gz
    ```
5. Run upgrade.sh
    ```shell
    $ bash flowgate/script/upgrade.sh
    ```
