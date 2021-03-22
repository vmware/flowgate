/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.client;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.vmware.flowgate.client.RestTemplateBuilder;
import com.vmware.flowgate.common.model.FacilitySoftwareConfig;
import com.vmware.flowgate.poweriqworker.model.Aisle;
import com.vmware.flowgate.poweriqworker.model.AislesResult;
import com.vmware.flowgate.poweriqworker.model.DataCenter;
import com.vmware.flowgate.poweriqworker.model.DataCentersResult;
import com.vmware.flowgate.poweriqworker.model.Floor;
import com.vmware.flowgate.poweriqworker.model.FloorsResult;
import com.vmware.flowgate.poweriqworker.model.Inlet;
import com.vmware.flowgate.poweriqworker.model.InletResult;
import com.vmware.flowgate.poweriqworker.model.Outlet;
import com.vmware.flowgate.poweriqworker.model.OutletsResult;
import com.vmware.flowgate.poweriqworker.model.Pdu;
import com.vmware.flowgate.poweriqworker.model.PduResult;
import com.vmware.flowgate.poweriqworker.model.PdusResult;
import com.vmware.flowgate.poweriqworker.model.Rack;
import com.vmware.flowgate.poweriqworker.model.RacksResult;
import com.vmware.flowgate.poweriqworker.model.Room;
import com.vmware.flowgate.poweriqworker.model.RoomsResult;
import com.vmware.flowgate.poweriqworker.model.Row;
import com.vmware.flowgate.poweriqworker.model.RowsResult;
import com.vmware.flowgate.poweriqworker.model.Sensor;
import com.vmware.flowgate.poweriqworker.model.SensorResult;
import com.vmware.flowgate.poweriqworker.model.SensorsResult;

public class PowerIQAPIClient {

   private static final Logger logger = LoggerFactory.getLogger(PowerIQAPIClient.class);

   private static final String GetPdusURL = "/api/v2/pdus";
   private static final String GetPduByIdURL = "/api/v2/pdus/%s";
   private static final String GetSensorsURL = "/api/v2/sensors";
   private static final String GetSensorsByPageURL = "/api/v2/sensors?order=id.asc&limit=%s&offset=%s";
   private static final String GetPDUsByPageURL = "/api/v2/pdus?order=id.asc&limit=%s&offset=%s";
   private static final String GetSensorByIdURL = "/api/v2/sensors/%s";

   private static final String GetRacksURL = "/api/v2/racks";

   private static final String GetRowsURL = "/api/v2/rows";

   private static final String GetAislesURL = "/api/v2/aisles";

   private static final String GetRoomsURL = "/api/v2/rooms";

   private static final String GetFloorsURL = "/api/v2/floors";

   private static final String GetDataCentersURL = "/api/v2/data_centers";

   private static final String GetOutletsByPDUURL = "/api/v2/pdus/%s/outlets";

   private static final String GetInletsByPDUURL = "/api/v2/pdus/%s/inlets";

   private String powerIQServiceEndpoint;

   private String username;

   private String password;

   private RestTemplate restTemplate;

   public PowerIQAPIClient() {
   }

   public PowerIQAPIClient(FacilitySoftwareConfig facilitySoftwareConfig) {
      this.username = facilitySoftwareConfig.getUserName();
      this.password = facilitySoftwareConfig.getPassword();
      this.powerIQServiceEndpoint = facilitySoftwareConfig.getServerURL();
      try {
         this.restTemplate =
               RestTemplateBuilder.buildTemplate(facilitySoftwareConfig.isVerifyCert(), 120000);
      } catch (Exception e) {
         logger.error("Error initializing the PowerIQAPIClient", e);
      }
   }

   public String getPowerIQServiceEndpoint() {
      return powerIQServiceEndpoint;
   }

   public RestTemplate getRestTemplate() {
      return restTemplate;
   }

   public void setRestTemplate(RestTemplate restTemplate) {
      this.restTemplate = restTemplate;
   }

   protected HttpHeaders buildHeaders() {
      HttpHeaders headers = RestTemplateBuilder.getDefaultHeader();
      String auth = this.username + ":" + this.password;
      byte[] encodeAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
      headers.add("Authorization", "Basic " + new String(encodeAuth));
      return headers;
   }

   protected HttpEntity<String> getDefaultEntity() {
      return new HttpEntity<String>(buildHeaders());
   }

   @Deprecated
   public List<Pdu> getPdus() {
      List<Pdu> pdus = new ArrayList<Pdu>();
      ResponseEntity<PdusResult> pdusResult =
            this.restTemplate.exchange(getPowerIQServiceEndpoint() + GetPdusURL, HttpMethod.GET,
                  getDefaultEntity(), PdusResult.class);
      if (pdusResult != null && pdusResult.getBody() != null
            && pdusResult.getBody().getPdus() != null) {
         pdus = pdusResult.getBody().getPdus();
      }
      return pdus;
   }

   public List<Pdu> getPdus(int limit, int offset) {
      ResponseEntity<PdusResult> pdusResult = this.restTemplate.exchange(
            getPowerIQServiceEndpoint() + String.format(GetPDUsByPageURL, limit, offset),
            HttpMethod.GET, getDefaultEntity(), PdusResult.class);
      if (pdusResult != null && pdusResult.getBody() != null) {
         return pdusResult.getBody().getPdus();
      }
      return null;
   }

   public Pdu getPduByID(long id) {
      ResponseEntity<PduResult> pduResult = this.restTemplate.exchange(
            getPowerIQServiceEndpoint() + String.format(GetPduByIdURL, id), HttpMethod.GET,
            getDefaultEntity(), PduResult.class);
      if (pduResult != null) {
         return pduResult.getBody().getPdu();
      }
      return null;
   }

   public List<Sensor> getSensors() {
      List<Sensor> sensors = new ArrayList<Sensor>();
      ResponseEntity<SensorsResult> sensorsResult =
            this.restTemplate.exchange(getPowerIQServiceEndpoint() + GetSensorsURL, HttpMethod.GET,
                  getDefaultEntity(), SensorsResult.class);
      if (sensorsResult != null && sensorsResult.getBody() != null
            && sensorsResult.getBody().getSensors() != null) {
         sensors = sensorsResult.getBody().getSensors();
      }
      return sensors;
   }

   public List<Sensor> getSensors(int limit, int offset) {
      List<Sensor> sensors = new ArrayList<Sensor>();
      ResponseEntity<SensorsResult> sensorsResult =
            this.restTemplate.exchange(getPowerIQServiceEndpoint() + String.format(GetSensorsByPageURL, limit, offset), HttpMethod.GET,
                  getDefaultEntity(), SensorsResult.class);
      if (sensorsResult != null && sensorsResult.getBody() != null
            && sensorsResult.getBody().getSensors() != null) {
         sensors = sensorsResult.getBody().getSensors();
      }
      return sensors;
   }

   public Sensor getSensorById(String id) {
      Sensor sensor = new Sensor();
      ResponseEntity<SensorResult> sensorResult = this.restTemplate.exchange(
            getPowerIQServiceEndpoint() + String.format(GetSensorByIdURL, id), HttpMethod.GET,
            getDefaultEntity(), SensorResult.class);
      if (sensorResult != null) {
         sensor = sensorResult.getBody().getSensor();
      }
      return sensor;
   }

   public List<Rack> getRacks() {
      List<Rack> racks = new ArrayList<Rack>();
      ResponseEntity<RacksResult> rackResult =
            this.restTemplate.exchange(getPowerIQServiceEndpoint() + GetRacksURL, HttpMethod.GET,
                  getDefaultEntity(), RacksResult.class);
      if (rackResult != null && rackResult.getBody() != null
            && rackResult.getBody().getRacks() != null) {
         racks = rackResult.getBody().getRacks();
      }
      return racks;
   }

   public List<Row> getRows() {
      List<Row> rows = new ArrayList<Row>();
      ResponseEntity<RowsResult> rowsResult =
            this.restTemplate.exchange(getPowerIQServiceEndpoint() + GetRowsURL, HttpMethod.GET,
                  getDefaultEntity(), RowsResult.class);
      if (rowsResult != null && rowsResult.getBody() != null
            && rowsResult.getBody().getRows() != null) {
         rows = rowsResult.getBody().getRows();
      }
      return rows;
   }

   public List<Aisle> getAisles() {
      List<Aisle> aisles = new ArrayList<Aisle>();
      ResponseEntity<AislesResult> aisleResult =
            this.restTemplate.exchange(getPowerIQServiceEndpoint() + GetAislesURL, HttpMethod.GET,
                  getDefaultEntity(), AislesResult.class);
      if (aisleResult != null && aisleResult.getBody() != null
            && aisleResult.getBody().getAisles() != null) {
         aisles = aisleResult.getBody().getAisles();
      }
      return aisles;
   }

   public List<Room> getRooms() {
      List<Room> rooms = new ArrayList<Room>();
      ResponseEntity<RoomsResult> roomsResult =
            this.restTemplate.exchange(getPowerIQServiceEndpoint() + GetRoomsURL, HttpMethod.GET,
                  getDefaultEntity(), RoomsResult.class);
      if (roomsResult != null && roomsResult.getBody() != null
            && roomsResult.getBody().getRooms() != null) {
         rooms = roomsResult.getBody().getRooms();
      }
      return rooms;
   }

   public List<Floor> getFloors() {
      List<Floor> floors = new ArrayList<Floor>();
      ResponseEntity<FloorsResult> floorsResult =
            this.restTemplate.exchange(getPowerIQServiceEndpoint() + GetFloorsURL, HttpMethod.GET,
                  getDefaultEntity(), FloorsResult.class);
      if (floorsResult != null && floorsResult.getBody() != null
            && floorsResult.getBody().getFloors() != null) {
         floors = floorsResult.getBody().getFloors();
      }
      return floors;
   }

   public List<DataCenter> getDataCenters() {
      List<DataCenter> dataCenters = new ArrayList<DataCenter>();
      ResponseEntity<DataCentersResult> dataCentersResult =
            this.restTemplate.exchange(getPowerIQServiceEndpoint() + GetDataCentersURL,
                  HttpMethod.GET, getDefaultEntity(), DataCentersResult.class);
      if (dataCentersResult != null && dataCentersResult.getBody() != null
            && dataCentersResult.getBody().getDataCenters() != null) {
         dataCenters = dataCentersResult.getBody().getDataCenters();
      }
      return dataCenters;
   }

   public List<Outlet> getOutlets(Long pduId){
      List<Outlet> outlets = new ArrayList<Outlet>();
      ResponseEntity<OutletsResult> outletsResult =
            this.restTemplate.exchange(getPowerIQServiceEndpoint() + String.format(GetOutletsByPDUURL, pduId),
                  HttpMethod.GET, getDefaultEntity(), OutletsResult.class);
      if (outletsResult != null && outletsResult.getBody() != null
            && outletsResult.getBody().getOutlets() != null) {
         outlets = outletsResult.getBody().getOutlets();
      }

      return outlets;
   }

   public List<Inlet> getInlets(long pduId){
      List<Inlet> inlets = new ArrayList<Inlet>();
      ResponseEntity<InletResult> inletsResult =
            this.restTemplate.exchange(getPowerIQServiceEndpoint() + String.format(GetInletsByPDUURL, pduId),
                  HttpMethod.GET, getDefaultEntity(), InletResult.class);
      if (inletsResult != null && inletsResult.getBody() != null
            && inletsResult.getBody().getInlets() != null) {
         inlets = inletsResult.getBody().getInlets();
      }
      return inlets;
   }

   public void testConnection() {
      getPdus(1, 1);
   }
}
