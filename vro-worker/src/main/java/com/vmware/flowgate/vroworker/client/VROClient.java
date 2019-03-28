/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.vroworker.client;

/**
 * @author admin
 *
 */
public class VROClient {
   //list all the resources withe hte adapter kind and resource key
  
  
   
   //list all the stat metrics
   ///api/resources/stats/latest?resourceId=UUID2&statKey=cpu|usageMhz&begin=&end= 
   //https://10.112.113.186/suite-api/api/resources/stats/latest?resourceId=61c28826-af26-4480-a600-6182d09d9fbb
   
   
   /**
    * 
 https://10.112.113.186/suite-api/api/resources/61c28826-af26-4480-a600-6182d09d9fbb/stats
 will post hte follow metric to the above resources
{
  "stat-content" : [ {
    "statKey" : "envrionment|temperature",
    "timestamps" : [ 1521187689000, 1521187719000, 1521187769000 ],
    "data" : [ 26, 24, 25 ],
    "others" : [ ],
    "otherAttributes" : {
    }
  }, {
    "statKey" : "envrionment|powerload",
    "timestamps" : [ 1521187689000, 1521187719000, 1521187769000,1521187779000 ],
    "data" : [ 93.0, 95.0, 97.0, 99.0 ],
    "others" : [ ],
    "otherAttributes" : {
    }
  } ]
}
    */
   /**
    * https://10.112.113.186/suite-api/api/resources/61c28826-af26-4480-a600-6182d09d9fbb/properties
{
  "property-content" : [ {
    "statKey" : "Location|Region",
    "timestamps" : [ 1521187689000],
    "values" : [ "APAC"],
    "others" : [ ],
    "otherAttributes" : {
    }
  }, {
    "statKey" : "Location|Country",
    "timestamps" : [ 1521187689000],
    "values" : ["China"],
    "others" : [ ],
    "otherAttributes" : {
    }
  },{
    "statKey" : "Location|City",
    "timestamps" : [ 1521187689000],
    "values" : ["Beijing"],
    "others" : [ ],
    "otherAttributes" : {
    }
  },{
    "statKey" : "Location|Building",
    "timestamps" : [ 1521187689000],
    "values" : ["S Wing of Tower C, Raycom InfoTech Park"],
    "others" : [ ],
    "otherAttributes" : {
    }
  },{
    "statKey" : "Location|Floor",
    "timestamps" : [ 1521187689000],
    "values" : ["9th"],
    "others" : [ ],
    "otherAttributes" : {
    }
  },{
    "statKey" : "Location|Room",
    "timestamps" : [ 1521187689000],
    "values" : ["Beijing Lab"],
    "others" : [ ],
    "otherAttributes" : {
    }
  },{
    "statKey" : "Location|CabinetName",
    "timestamps" : [ 1521187689000],
    "values" : ["R04"],
    "others" : [ ],
    "otherAttributes" : {
    }
  },{
    "statKey" : "Location|CabinetUNumber",
    "timestamps" : [ 1521187689000],
    "data" : [29],
    "others" : [ ],
    "otherAttributes" : {
    }
  }  ]
}
    */
}
