- [User Guide](#user-guide)
  - [User Management](#user-management)
    - [Create User](#create-user)
    - [Create Role](#create-role)
  - [Datacenter Facility System Integration](#datacenter-facility-system-integration)
    - [Integrate with Nlyte](#integrate-with-nlyte)
    - [Integrate with PowerIQ](#integrate-with-poweriq)
    - [Integrate with Openmanage](#integrate-with-openmanage)
    - [Integrate with Infoblox](#integrate-with-infoblox)
    - [Data Center Facility Systems management](#data-center-facility-systems-management)
    - [Edit](#edit)
    - [Delete](#delete)
    - [Sync Data](#sync-data)
    - [Pending/Resume Data syncing](#pendingresume-data-syncing)
    - [Status Monitor](#status-monitor)
  - [Setting](#setting)
    - [Summary page](#summary-page)
      - [View Facility information by system](#view-facility-information-by-system)
    - [View Asset Topology by vCenter](#view-asset-topology-by-vcenter)
    - [Sync Data](#sync-data-1)
      - [Aggregate Server Mapping job](#aggregate-server-mapping-job)
      - [Aggregate PDUs and Server Mapping job](#aggregate-pdus-and-server-mapping-job)
      - [Sync Server and Sensor Mapping Job](#sync-server-and-sensor-mapping-job)
    - [List UN-mapped servers](#list-un-mapped-servers)
    - [Asset Name and IP Mapping](#asset-name-and-ip-mapping)
      - [Create Mapping](#create-mapping)
      - [Edit Mapping](#edit-mapping)
    - [Asset Management](#asset-management)
      - [Add Asset](#add-asset)
      - [Edit Asset](#edit-asset)
      - [Delete Asset](#delete-asset)
    - [Custom Facility Adapter Management](#custom-facility-adapter-management)
  - [IT System Integration Management](#it-system-integration-management)
    - [IT system integration](#it-system-integration)
    - [Integrate with Aria Operations](#integrate-with-aria-operations)
    - [Integrate with vCenter](#integrate-with-vcenter)
    - [Edit](#edit-1)
    - [Delete](#delete-1)
    - [Sync data](#sync-data)
    - [Re-Active an Integration.](#re-active-an-integration)
    - [Validate data](#validate-data)
      - [vCenter](#vcenter)
      - [Aria Operation Manager](#aria-operation-manager)
  - [View/Manage Server Mapping](#viewmanage-server-mapping)
    - [Create Mapping](#create-mapping-1)
    - [Mapping Server Asset](#mapping-server-asset)
    - [Update server mapping](#update-server-mapping)
    - [Mapping PDUs for Server](#mapping-pdus-for-server)
    - [Mapping Sensors for Server](#mapping-sensors-for-server)
    - [Mapping Switches for Server](#mapping-switches-for-server)
    - [Delete Server Mapping](#delete-server-mapping)
  - [API](#api)

## User Guide
### User Management
Login with admin user and password

<img alt="login" src="images/user guide/login.png">

#### Create User

You can create a user by click the button “CREATE A USER”

<img alt="login" src="images/user guide/userlist.png">

Three default roles have been created. 

*	Admin: The system administrator role
*	User: Role for general User to access Flowgate through UI
*	API: role for user who want to access Flowgate through API

Select one or multiple roles for the new user.

<img alt="adduser" src="images/user guide/adduser.png">

#### Create Role
Admin can create new role according to the permission requirement. 

<img alt="rolelist" src="images/user guide/rolelist.png">

Select privileges for the new role

<img alt="addrole" src="images/user guide/addrole.png">
<img alt="addrole" src="images/user guide/addrole1.png">
<img alt="addrole" src="images/user guide/addrole2.png">

Click Next to enter new role name.

<img alt="addrole" src="images/user guide/addrole3.png">

Click finish to create the role.

<img alt="rolelist" src="images/user guide/rolelist1.png">

Click “SHOW MORE” to view the role privileges. 

<img alt="rolelist1show" src="images/user guide/rolelist1show.png">

### Datacenter Facility System Integration
Flowgate itself doesn’t have any data. Before IT users can use it. Flowgate Administrator need to integrate Facility systems first.  Currently it supports integrate with Nlyte, PowerIQ, Infoblox and Labsdb by default. More facility systems will be supported later.  As far as Flowgate is an opensource project, user can develop their system adapter easily when needed.

#### Integrate with Nlyte
1. Navigate to Data Center Facility->DCIM click the “ADD NEW INTEGRATION” button.

<img alt="dcimlist" src="images/user guide/dcimlist.png">

2. Select the “Type” as Nlyte.
3. Input the Nlyte server address, it can be either an IP or a FQDN address.
4. Input other fields value accordingly
5. For the VerifyCert option. If your Nlyte use a self-signed certificate, you should choose “no” which means not verify the certificate, otherwise Flowgate will not be able to connect to the server.
6. Click “SUBMIT” to save the setting. If any error happens, please update the input fields according to the error message.

If you have multiple Nlyte server then repeat the steps in 1~6.

<img alt="addDcim" src="images/user guide/addDcim.png">

If the metric unit in your Nlyte is different with the default ones. Please modify it accordingly. You can contact your Nlyte administrator for the default metric unit. 

<img alt="addDcim" src="images/user guide/addDcim1.png">

#### Integrate with PowerIQ

1. Navigate to “Data Center Facility->DCIM” click the “ADD NEW INTEGRATION” button.
2. Select Type as "PowerIQ"
3. Input Server IP (Or FQDN), Name, Username, Password and other fields.
4. For the VerifyCert option. If your PowerIQ use a self-signed certificate, you should choose “no” which means not verify the certificate, otherwise Flowgate will not be able to connect to the server.
5. Click “SUBMIT” to save the setting. If any error happens, please update the related fields according to the error message.

If you have multiple PowerIQ servers, repeat step 1-5.

<img alt="addPowerIq" src="images/user guide/addPowerIq.png">

Click the “Advance Setting” to check the default metric unit for your PowerIQ server. Please contact to your PowerIQ administrator to get the correct unit. 

<img alt="addPowerIq" src="images/user guide/addPowerIQ1.png">

#### Integrate with Openmanage

1. Navigate to “Data Center Facility->DCIM” click the “ADD NEW INTEGRATION” button.
2. Select Type as "Openmanage"
3. Input Server IP (Or FQDN), Name, Username, Password and other fields.
4. For the VerifyCert option. If your Openmanage use a self-signed certificate, you should choose “no” which means not verify the certificate, otherwise Flowgate will not be able to connect to the server.
5. Click “SUBMIT” to save the setting. If any error happens, please update the related fields according to the error message.

If you have multiple Openmanage servers, repeat step 1-5.

<img alt="openmanage" src="images/user guide/openmanage.png">

#### Integrate with Infoblox

1. Navigate to “Data Center Facility -> CMDB” click the “ADD NEW INTEGRATION” button.
2. Select Type as “InfoBlox”
3. Input Server IP (Or FQDN), Name, Username, Password and other fields.
4. For the VerifyCert option. If your Infoblox use a self-signed certificate, you should choose “no” which means not verify the certificate. Otherwise Flowgate will not be able to connect to the server.
5. Click “SUBMIT” to save the setting. If any error happens, please update the related fields according to the error message.

If you have multiple Infoblox servers, repeat step 1-5.

<img alt="addInfoblox" src="images/user guide/addCmdb.png">

#### Data Center Facility Systems management

Navigate to “Data Center Facility->DCIM”

#### Edit

Administrator can update the Facility system information when required. 

#### Delete

Administrator can delete a facility system integration.

#### Sync Data

Administrator can force to sync data for each facility system with “Sync Data” button. This will trigger a job to pull all the data from related facility system immediately. Please Note we have backend jobs to sync the data in different frequency. E.g. static data will be synced daily. Sensor data will sync every 5 minutes. For most of the case you don’t need to sync the data manually.

<img alt="dcimlist" src="images/user guide/dcimlistnotnull.png">

#### Pending/Resume Data syncing

Administrator can stop a system’s data syncing by click the “Pending” button. You can re-active a pending integration by click the “Active” button.

#### Status Monitor

The facility systems’ status can be monitored by the “Status” column. When error happens click the red “!” flag will show the detail information. Follow the instruction to fix the issue.  After you fix the issue, you need to click the “Active” button to re-active the integration.

<img alt="dcimliststatus" src="images/user guide/dcimliststatus.png">

### Setting

#### Summary page

<img alt="summary" src="images/user guide/summary.png">

##### View Facility information by system

Click on 'SYSTEM' card, the Data Center Facility System page will show up.

<img alt="summary_system" src="images/user guide/summary_system.png">
 
The facility information will be displayed group by system. If a facility items showed up in different systems, it will only count once in the total number. 
 
<img alt="summary_facility" src="images/user guide/summary_facility.png">
 
##### View Sensor summary 

Click the ‘SENSORS’ card will show the sensor summary.

<img alt="summary_sensor" src="images/user guide/summary_sensor.png">

Currently it only counts the humidity, temperature, airflow, smoke and water sensors. More sensor type will be supported later. 

<img alt="summary_sensor_detail" src="images/user guide/summary_sensor_detail.png">

#### View Asset Topology by vCenter

Click “System Setting -> Asset Topology”, select a vCenter server. It will display the asset topology for the vCenter
 
 <img alt="system_setting_asset_topology" src="images/user guide/system_setting_asset_topology.png">
 
Servers, PDU and switch connection information. Take host 10.192.76.21 for example from the topology diagram we can see that it connects to network switch “switch02” on port 02. It also connects to PDU sc1-g03-pdu-red on port 23 and PDU sc1-g07-pdu on port 18 as the follow figure shows.
 
<img alt="system_setting_asset_topology" src="images/user guide/system_setting_asset_topology1.png">

#### Sync Data

##### Aggregate Server Mapping job 
 
 <img alt="system_setting_aggregate_job" src="images/user guide/system_setting_aggregate_job.png">

* This job is used for aggregate server mapping items between different IT systems. Administrator can run this job to reduce the mapping items.

* This job will automatically be executed weekly. Administrator can trigger it manually anytime when required.

* For example, if an ESXi host is managing by vCenter A and Aria Operations B. Both vCenter A and Aria Operations B registered to Flowgate. Flowgate will create two mapping items for the same ESXi host.  When the job runs it will merge the two items into one.

##### Aggregate PDUs and Server Mapping job

“Setting->System Setting ->SyncData” Click the “TRIGGER” button for the “Trigger aggregate PDUs and Server Mapping job”

* This Job is used to create the mappings between PDUs and Server.
* After you trigger this job. You can find that PDUs filed in vCenter will be filled. Please NOTE this job will also be automatically executed hourly in the backend.  
* Before running the job, you will find the Asset.PDUs field in the Custom Attributes section is empty.

<img alt="vcenter" src="images/user guide/vcenter.png">

After running the job, the Asset.PDUs field is filled with value “sha1-r17-l, sha1-r17-r”.  it means it connected to two PDU. “sha1-r17-l, sha1-r17-r" are the names of the two PDU.

<img alt="vcenter" src="images/user guide/vcenter1.png">

##### Sync Server and Sensor Mapping Job
* This job is used to create the mapping between the sensors in datacenter and servers. Flowgate will use some most possible algorithm to calculate the sensors that can indicate the servers' environment.
* This job will also be running hourly in the backend automatically. But you can always trigger it mannully when required.

#### List UN-mapped servers 
List out all the servers that not mapped to a facility asset yet.  Usually this mean that Flowgate cannot automatically create the mapping between a host in IT system and a server in the facility system. 

<img alt="system_setting_unmapped" src="images/user guide/system_setting_unmapped.png">

When this happened it usually mean there are some data mismatch between the facility systems. Facility system administrator can use this to check the related system. 

#### Asset Name and IP Mapping

<img alt="system_setting_asset_Ip_mapping" src="images/user guide/system_setting_asset_Ip_mapping.png">
 
In Flowgate we use IP address and assetName to create the connections between IT system and Facility system. In case that we don’t have IPAM system(eg, Infoblox) , we can manully create the mapping relations here.

##### Create Mapping 

1.	Click ADD button.
2.	Input a valid IP address.
3.	Type the AssetName, select the right one from the dropdown list.
4.	Click “Save” to save the mapping.
 
<img alt="system_setting_asset_Ip_mapping" src="images/user guide/system_setting_asset_Ip_mapping1.png">
 
##### Search Mappings by IP

1.	Input an IP address
2.	Click “SEARCH” button

<img alt="system_setting_asset_Ip_mapping_list" src="images/user guide/system_setting_asset_Ip_mapping_list.png">

##### Edit Mapping

1.	Select a mapping item.
2.	Click “EDIT” button
3.	Input the new mapping information.

<img alt="system_setting_asset_Ip_mapping_edit" src="images/user guide/system_setting_asset_Ip_mapping_edit.png">
 
##### Delete a mapping

1.	Select a mapping item.
2.	Click “DELETE” button
 
<img alt="system_setting_asset_Ip_mapping_delete" src="images/user guide/system_setting_asset_Ip_mapping_delete.png">
 
##### Batch add mappings

1.	Click “BATCH ADD” button
 
<img alt="system_setting_asset_Ip_mapping_batch" src="images/user guide/system_setting_asset_Ip_mapping_batch.png">

<img alt="system_setting_asset_Ip_mapping_batch" src="images/user guide/system_setting_asset_Ip_mapping_batch1.png">

2.	Select a mapping file contains IP addresses and hostnames.
Each line of the file should only contain one IP and AssetName pair, they should be separated by a whitespace, for example:
IP1 assetName1
IP2 assetName2
3.	Click “UPLOAD” button to import the mappings.
4.	In case you see the follow screen, it means that some item in the file are not valid. Please check it accordingly and import the failed items again.

<img alt="system_setting_asset_Ip_mapping_batch" src="images/user guide/system_setting_asset_Ip_mapping_batch2.png">

#### Asset Management

User can manage the assets created through the UI here. Other assets feed by DCIM adapters are not supported.

##### Add Asset

1. Click "ADD NEW ASSET" button
<img alt="asset_management" src="images/user guide/asset_management.png">

2. Fill in asset information
<img alt="asset_management" src="images/user guide/asset_management_add_info.png">

3. Add asset location and click "Submit" button
<img alt="asset_management" src="images/user guide/asset_management_add_location.png">

4. Then you can see it in the asset list
<img alt="asset_management" src="images/user guide/asset_management_list.png">

##### Edit Asset

1. Click <img alt="asset_management" src="images/user guide/edit_icon.png"> and click "Edit" button
<img alt="asset_management" src="images/user guide/asset_management_edit.png">

2. Edit your asset
<img alt="asset_management" src="images/user guide/asset_management_edit_info.png">

3. You can also edit json directly through advanced function
<img alt="asset_management" src="images/user guide/asset_management_edit_advance.png">


##### Delete Asset

1. Click <img alt="asset_management" src="images/user guide/edit_icon.png"> and click "delete" button
<img alt="asset_management" src="images/user guide/asset_management_delete.png">

2. Click "OK" to delete asset
<img alt="asset_management" src="images/user guide/asset_management_delete_tips.png">

#### Custom Facility Adapter Management

1. Please refer to **[Custom Adapter Guide](adapter_guide.md)**.

### IT System Integration Management

#### IT system integration 

IT System admin can integrate their IT systems to Flowgate, after that Flowgate will push facility data to IT system accordingly.

<img alt="sddc_list" src="images/user guide/sddc_list.png">

#### Integrate with Aria Operations

1.	Navigate to IT Management -> VMware and click the “ADD NEW INTEGRATION” button
2.	Select the server Type: Aria Operation Manager.
3.	Input the server IP (or FQDN)
4.	Give a name for this Integration
5.	Input server username and password.
6.	For the VerifyCert option. If your Aria Operation Manager use a self-signed certificate, you should choose “no” which means not verify the certificate. Otherwise Flowgate will not be able to connect to the server.
7.	Click “SBUMIT” to finish the integration. 

Repeat step 1-7 if you have more than one Aria Operation Manager servers. 
 
<img alt="sddc_add" src="images/user guide/sddc_add.png">

#### Integrate with vCenter

1.	Navigate to IT Management -> VMware and click the “ADD NEW INTEGRATION” button
2.	Select the server Type: vCenter Server.
3.	Input the server IP (or FQDN)
4.	Give a name for this Integration
5.	Input server username and password.
6.	For the VerifyCert option. If your vCenter server use a self-signed certificate, you should choose “no” which means not verify the certificate. Otherwise Flowgate will not be able to connect to the server.
7.	Click “SBUMIT” to finish the integration.

If you have more vCenter servers, then repeat the above step 1-7.
 
<img alt="sddc_add" src="images/user guide/sddc_add2.png">

#### Edit  

Click the “Dot Button” and select the “Edit” menu to update the integration information when needed.

#### Delete

Click the “Dot Button” and select “Delete” menu to delete an integration.

#### Sync data

User can force Flowgate to push the latest data to its IT system on demand. By default, Flowgate will push the last data to IT system every 5 minutes. 
 
<img alt="sddc_syncjob" src="images/user guide/sddc_syncjob.png">
  
#### Pending an Integration

User can suspend an integration on demand.  When an integration in pending status it will not sending data anymore.

#### Re-Active an Integration.

User can re-active an integration which is in ERROR or Pending status

#### Validate data

##### vCenter

After create an integration you can view host’s metadata in the Customer attributes section.  
 
 <img alt="vcenter" src="images/user guide/vcenter2.png">

##### Aria Operation Manager
User can view the environment metrics in the metric section. You can also view the host’s metadata in the property section. User can create alert base on the new metrics feed by Flowgate. 
 
<img alt="vrops" src="images/user guide/vrop.png">
<img alt="vrops" src="images/user guide/vrops1.png">

By default, Flowgate has created 3 predefined alerts for user.
 
<img alt="vrops_alert" src="images/user guide/vrops_alert.png">

 
### View/Manage Server Mapping

After register IT systems, you can view the server mapping information in Server Mapping section.
 
<img alt="server_mapping" src="images/user guide/server_mapping.png">

#### Create Mapping

By default, the server mapping will be created automatically by Flowgate. In case of the system cannot create the mapping. Flowgate provide manually mapping functionality. User can search the Asset by name and then create the mapping.

#### Mapping Server Asset

1.	Click “ACTION” -> “Mapping Server asset”
2.	Search the Server Asset by name. 
3.	Select the right server and click OK( you can identify the server by Location, SN, Tag, Cabinet information, etc)
 
 <img alt="server_mapping" src="images/user guide/server_mapping_server.png">
 <img alt="server_mapping" src="images/user guide/server_mapping_server1.png">
 
#### Update server mapping

1.	Click “ACTION” -> “Mapping Server asset”
2.	Search the Server Asset By name.
3.	Select the new Server Asset and click OK.

<img alt="server_mapping" src="images/user guide/server_mapping_other.png">

4.	If you just want to cancel the mapping, you can uncheck the “check box” after the “SEARCH” button then click “OK”.
 
 <img alt="server_mapping" src="images/user guide/server_mapping_server2.png">
 
#### Mapping PDUs for Server

1.	Click “ACTION” -> ”Mapping PDU asset”
2.	Search the PDU by name.
3.	Select the PDUs that connected to the Server. You need to contact you Datacenter Operator to get the correct PUD items. 
4.	Click “OK” to Save the mapping.
 
 <img alt="server_mapping_pdu" src="images/user guide/server_mapping_pdu.png">
 
#### Mapping Sensors for Server
1.	Select a Metric and click the “MAPPING SENSOR” button.
	 
	 <img alt="server_mapping_sensor" src="images/user guide/server_mapping_sensor.png">
2.	Search the sensor by name.
3.	Select the sensor for the metric and click “OK”.
 
 <img alt="server_mapping_sensor" src="images/user guide/server_mapping_sensor1.png">
 <img alt="server_mapping_sensor" src="images/user guide/server_mapping_sensor2.png">
 
#### Mapping Switches for Server

1.	Click “ACTION” -> “Mapping Switch asset” button
2.	Search switches by name.
3.	Select the switches connected to the server and click OK.
 
<img alt="server_mapping_switch" src="images/user guide/server_mapping_switch.png">

#### Delete Server Mapping

1.	Click “ACTIONS” -> “Delete” button.
 
 <img alt="server_mapping_delete" src="images/user guide/server_mapping_delete.png">
 
2.	Click OK button in the confirm dialog. 
 
 <img alt="server_mapping_delete" src="images/user guide/server_mapping_delete1.png">

### API 

User can view the API DOC on the right up corner. Fully functional restful API for all Flowgate functionalities. 
 
 <img alt="flowgate_api" src="images/user guide/flowgate_api.png">
 <img alt="flowgate_api" src="images/user guide/flowgate_api1.png">
 
### User Profile

User can change the password in the User Profile page.
 
 <img alt="userprofile" src="images/user guide/userprofile.png">


