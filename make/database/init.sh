#!/bin/bash
#
#Copyright 2019 VMware, Inc.
#SPDX-License-Identifier: BSD-2-Clause
#
/opt/couchbase/bin/couchbase-cli cluster-init --cluster-username=flowgate_cluster --cluster-password=ADMINPASSWD_CHANGE --cluster-ramsize=4096 --cluster-index-ramsize=2048 --cluster-fts-ramsize=1024 --index-storage-setting=default --services=data,index,query
/opt/couchbase/bin/couchbase-cli bucket-create -c localhost -u flowgate_cluster -p ADMINPASSWD_CHANGE --bucket flowgate --bucket-type couchbase --bucket-ramsize 4096 --bucket-replica 1 --wait
/opt/couchbase/bin/couchbase-cli user-manage -c localhost -u flowgate_cluster -p ADMINPASSWD_CHANGE --set --rbac-username=flowgate --rbac-password=USERPASSWD_CHANGE --roles=bucket_full_access[*] --auth-domain=local

###create index###
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE -file /home/couchbase/initData.sh

touch /opt/couchbase/var/initDataComplete