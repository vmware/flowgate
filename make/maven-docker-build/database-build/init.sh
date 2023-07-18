#!/bin/bash
#
#Copyright 2019 VMware, Inc.
#SPDX-License-Identifier: BSD-2-Clause
#
/opt/couchbase/bin/couchbase-cli cluster-init --cluster-username=flowgatetest --cluster-password=COUCHBASEPASSWD_CHANGE --cluster-ramsize=1024 --cluster-index-ramsize=512 --cluster-fts-ramsize=256 --index-storage-setting=default --services=data,index,query
/opt/couchbase/bin/couchbase-cli user-manage -c localhost -u flowgatetest -p COUCHBASEPASSWD_CHANGE --set --rbac-username=flowgatetest --rbac-password=COUCHBASEPASSWD_CHANGE --roles=bucket_full_access[*] --auth-domain=local
/opt/couchbase/bin/couchbase-cli bucket-create -c localhost -u flowgatetest -p COUCHBASEPASSWD_CHANGE --bucket flowgatetest --bucket-type couchbase --bucket-ramsize 512 --bucket-replica 1 --wait
/opt/couchbase/bin/cbq -u flowgatetest -p COUCHBASEPASSWD_CHANGE --script="CREATE INDEX \`class_index\` ON \`flowgatetest\`(\`_class\`) USING GSI WITH {\"defer_build\":false}"