#!/bin/bash
#
#Copyright 2019 VMware, Inc.
#SPDX-License-Identifier: BSD-2-Clause
#
/opt/couchbase/bin/couchbase-cli cluster-init --cluster-username=flowgate_cluster --cluster-password=ADMINPASSWD_CHANGE --cluster-ramsize=4096 --cluster-index-ramsize=2048 --cluster-fts-ramsize=1024 --index-storage-setting=default --services=data,index,query
/opt/couchbase/bin/couchbase-cli bucket-create -c localhost -u flowgate_cluster -p ADMINPASSWD_CHANGE --bucket flowgate --bucket-type couchbase --bucket-ramsize 4096 --bucket-replica 1 --wait
/opt/couchbase/bin/couchbase-cli user-manage -c localhost -u flowgate_cluster -p ADMINPASSWD_CHANGE --set --rbac-username=flowgate --rbac-password=USERPASSWD_CHANGE --roles=bucket_full_access[*] --auth-domain=local

# Wait for the query service to be up and running
for attempt in $(seq 5)
do
    RET=`cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script="select count(*) from flowgate;" | grep -A 3 results | grep '"$1":'`
    if [ -n "$RET" ] && [ ${RET#*:} = "0" ];then
        echo "Service start success."
        break
    fi
    echo "Waiting for query service..."
    sleep 1
done

###create index###
cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE -file /home/couchbase/initData.sh > /opt/couchbase/var/couchbase_initialize.log

DATACOUNT=`cbq -u flowgate_cluster -p ADMINPASSWD_CHANGE --script="select count(*) from flowgate;" | grep -A 3 results | grep '"$1":'`
REALDATACOUNT=`grep '\<INSERT INTO flowgate\>' /home/couchbase/initData.sh | grep -Ev "^$|^[#;]" | wc -l`

if [ -n "$DATACOUNT" ] && [ ${DATACOUNT#*:} = "$REALDATACOUNT" ];then
    touch /opt/couchbase/var/initDataComplete
else
    touch /opt/couchbase/var/initDataFailed
fi
