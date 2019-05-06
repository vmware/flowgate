#!/bin/bash
#
#Copyright 2019 VMware, Inc.
#SPDX-License-Identifier: BSD-2-Clause
#
/opt/couchbase/bin/couchbase-cli cluster-init --cluster-username=flowgate --cluster-password=ADMINPASSWD_CHANGE --cluster-ramsize=1024 --cluster-index-ramsize=512 --cluster-fts-ramsize=256 --index-storage-setting=default --services=data,index,query
/opt/couchbase/bin/couchbase-cli user-manage -c localhost -u flowgate -p ADMINPASSWD_CHANGE --set --rbac-username=flowgate --rbac-password=USERPASSWD_CHANGE --roles=bucket_full_access[*] --auth-domain=local
/opt/couchbase/bin/couchbase-cli bucket-create -c localhost -u flowgate -p ADMINPASSWD_CHANGE --bucket flowgate --bucket-type couchbase --bucket-ramsize 512 --bucket-replica 1 --wait
bash /home/couchbase/initData.sh
/opt/couchbase/bin/cbdocloader -c localhost:8091 -u flowgate -p ADMINPASSWD_CHANGE -b flowgate -m 512 -d ./database.zip