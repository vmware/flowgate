db.auth('wormhole', 'USERPASSWD_CHANGE')
db = db.getSiblingDB('wormhole')

db.asset.updateMany({"_class":"com.vmware.wormhole.common.model.Asset"},{$set:{"_class":"com.vmware.flowgate.common.model.Asset"}});
db.facilitySoftwareConfig.updateMany({"_class":"com.vmware.wormhole.common.model.FacilitySoftwareConfig"},{$set:{"_class":"com.vmware.flowgate.common.model.FacilitySoftwareConfig"}});
db.realTimeData.updateMany({"_class":"com.vmware.wormhole.common.model.RealTimeData"},{$set:{"_class":"com.vmware.flowgate.common.model.RealTimeData"}});
db.sDDCSoftwareConfig.updateMany({"_class":"com.vmware.wormhole.common.model.SDDCSoftwareConfig"},{$set:{"_class":"com.vmware.flowgate.common.model.SDDCSoftwareConfig"}});
db.sensorSetting.updateMany({"_class":"com.vmware.wormhole.common.model.SensorSetting"},{$set:{"_class":"com.vmware.flowgate.common.model.SensorSetting"}});
db.serverMapping.updateMany({"_class":"com.vmware.wormhole.common.model.ServerMapping"},{$set:{"_class":"com.vmware.flowgate.common.model.ServerMapping"}});
db.wormholeUser.updateMany({"_class":"com.vmware.wormhole.common.model.WormholeUser"},{$set:{"_class":"com.vmware.flowgate.common.model.WormholeUser"}});

db.jobConfig.remove({"_id":"VRO-PREDEFINED-METRIC-PROPERTIES-ALERTS-SYNC"});
db.jobConfig.remove({"_id":"VC-PREDEFINED-CUSTOM-ATTRIBUTES-SYNC"});
db.jobConfig.remove({"_id":"NLYTE-PREDEFINED-FETCH-ASSET-TO-WORMHOLE"});
db.jobConfig.remove({"_id":"NLYTE-PREDEFINED-UPDATE-MAPPEDASSET-TO-WORMHOLE"});
db.jobConfig.remove({"_id":"NLYTE-PREDEFINED-Sync-REALTIMEDATA"});
db.jobConfig.remove({"_id":"POWERIQ-SYNC-REALTIMEDATA"});
db.jobConfig.remove({"_id":"POWERIQ-SYNC-SENSORDATA"});

db.asset.remove({"category":"Sensors"});