#!/bin/bash
#
#Copyright 2019 VMware, Inc.
#SPDX-License-Identifier: BSD-2-Clause
#
/opt/couchbase/bin/couchbase-cli cluster-init --cluster-username=flowgate_cluster --cluster-password=ADMINPASSWD_CHANGE --cluster-ramsize=4096 --cluster-index-ramsize=2048 --cluster-fts-ramsize=1024 --index-storage-setting=default --services=data,index,query
/opt/couchbase/bin/couchbase-cli bucket-create -c localhost -u flowgate_cluster -p ADMINPASSWD_CHANGE --bucket flowgate --bucket-type couchbase --bucket-ramsize 4096 --bucket-replica 1 --wait
/opt/couchbase/bin/couchbase-cli user-manage -c localhost -u flowgate_cluster -p ADMINPASSWD_CHANGE --set --rbac-username=flowgate --rbac-password=USERPASSWD_CHANGE --roles=bucket_full_access[*] --auth-domain=local
bash /home/couchbase/initData.sh
/opt/couchbase/bin/cbdocloader -c localhost -u flowgate -p USERPASSWD_CHANGE -b flowgate -m 4096 -d ./database.zip

###create index###
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script='CREATE INDEX `userName_index` ON `flowgate`(`userName`, `_class`) where _class = "com.vmware.flowgate.common.model.WormholeUser"'
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script='CREATE INDEX `sourcecategory_index` ON `flowgate`(`assetSource`,`category`) WHERE `_class` = "com.vmware.flowgate.common.model.Asset"'
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script='CREATE INDEX `realTimeData_index` ON `flowgate`(`assetID`,`_class`,`time`) WHERE `_class` = "com.vmware.flowgate.common.model.RealTimeData"'
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script='CREATE INDEX `assetNameLike_index` ON `flowgate`(`_class`,`assetName`,`category`) WHERE `_class` = "com.vmware.flowgate.common.model.Asset"'
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script='CREATE INDEX `tpye_index` ON `flowgate`(`_class`,`category`) WHERE `_class` = "com.vmware.flowgate.common.model.Asset"'
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script='CREATE INDEX `vro_vc_index` ON `flowgate`(`vcID`,`vroID`) WHERE `_class` = "com.vmware.flowgate.common.model.ServerMapping"'
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script='CREATE INDEX `sddc_index` ON `flowgate`(`type`) WHERE `_class` = "com.vmware.flowgate.common.model.SDDCSoftwareConfig"'
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script='CREATE INDEX `like_index` ON `flowgate`(`category`,`assetName`) WHERE `_class` = "com.vmware.flowgate.common.model.Asset"'
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script='CREATE INDEX `facilityType_index` ON `flowgate`(`type`) WHERE `_class` = "com.vmware.flowgate.common.model.FacilitySoftwareConfig"'
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script='CREATE INDEX `jobconfig_index` ON `flowgate`(`jobType`) WHERE `_class` = "com.vmware.flowgate.common.model.JobConfig"'
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script='CREATE INDEX `queryByAssetName_index` ON `flowgate`(`assetName`) WHERE `_class` = "com.vmware.flowgate.common.model.Asset"'
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script='CREATE INDEX `class_index` ON `flowgate`(`_class`)'

touch /opt/couchbase/var/initDataComplete