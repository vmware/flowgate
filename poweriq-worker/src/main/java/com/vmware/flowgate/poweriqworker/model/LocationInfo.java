package com.vmware.flowgate.poweriqworker.model;

import java.util.Map;

public class LocationInfo {

   private Map<Long, Rack> racksMap;
   private Map<Long, Row> rowsMap;
   private Map<Long, Aisle> aislesMap;
   private Map<Long, Room> roomsMap;
   private Map<Long, Floor> floorsMap;
   private Map<Long, DataCenter> dataCentersMap;

   public Map<Long, Rack> getRacksMap() {
      return racksMap;
   }
   public void setRacksMap(Map<Long, Rack> racksMap) {
      this.racksMap = racksMap;
   }
   public Map<Long, Row> getRowsMap() {
      return rowsMap;
   }
   public void setRowsMap(Map<Long, Row> rowsMap) {
      this.rowsMap = rowsMap;
   }
   public Map<Long, Aisle> getAislesMap() {
      return aislesMap;
   }
   public void setAislesMap(Map<Long, Aisle> aislesMap) {
      this.aislesMap = aislesMap;
   }
   public Map<Long, Room> getRoomsMap() {
      return roomsMap;
   }
   public void setRoomsMap(Map<Long, Room> roomsMap) {
      this.roomsMap = roomsMap;
   }
   public Map<Long, Floor> getFloorsMap() {
      return floorsMap;
   }
   public void setFloorsMap(Map<Long, Floor> floorsMap) {
      this.floorsMap = floorsMap;
   }
   public Map<Long, DataCenter> getDataCentersMap() {
      return dataCentersMap;
   }
   public void setDataCentersMap(Map<Long, DataCenter> dataCentersMap) {
      this.dataCentersMap = dataCentersMap;
   }

}
