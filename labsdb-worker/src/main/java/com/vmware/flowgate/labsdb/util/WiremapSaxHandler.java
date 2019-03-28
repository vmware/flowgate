/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.labsdb.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.vmware.flowgate.labsdb.common.EndDevice;
import com.vmware.flowgate.labsdb.common.EndDevice.WireMapType;

public class WiremapSaxHandler extends DefaultHandler {
   
   private List<EndDevice> endDevices;
   private EndDevice endDevice;
   private String currentTag = null;
   private String nodeName;
   private StringBuilder currentValue;
   private static final String attributeName = "name";
   private static final String WIRE_TYPE_TAG = "WIRE_TYPE";
   private static final String CONNECTS_TAG = "CONNECTS";
   private static final String Connects_Value_Split_Regex = ":";
   private static Map<String,WireMapType> wireMapType = new HashMap<String,WireMapType>();
   static {
      wireMapType.put("fiber", WireMapType.fiber);
      wireMapType.put("ilo", WireMapType.ilo);
      wireMapType.put("iscsi", WireMapType.iscsi);
      wireMapType.put("power", WireMapType.power);
      wireMapType.put("net", WireMapType.net);
      wireMapType.put("serial", WireMapType.serial);
      wireMapType = Collections.unmodifiableMap(wireMapType);
   }

   public WiremapSaxHandler() {
   }

   public WiremapSaxHandler(String nodeName) {
      this.nodeName = nodeName;
   }

   public List<EndDevice> getEndDevices() {
      return endDevices;
   }
   
   @Override
   public void startDocument() throws SAXException {
       super.startDocument();
       endDevices = new ArrayList<EndDevice>();
   }
 
   @Override
   public void endDocument() throws SAXException {
       super.endDocument();
   }

   @Override
   public void startElement(String uri, String localName, String name,
           Attributes attributes) throws SAXException {
       super.startElement(uri, localName, name, attributes);
       if (name.equals(nodeName)) {
          endDevice = new EndDevice();
          for (int i = 0; i < attributes.getLength(); i++) {
             if(attributeName.equals(attributes.getLocalName(i))){
                 endDevice.setStartPort(attributes.getValue(i));
             }
         }
       }
       currentTag = name;
       currentValue = new StringBuilder();
   }
   
   @Override
   public void characters(char[] ch, int start, int length)
           throws SAXException {
       super.characters(ch, start, length);
       String value = "";
       if (currentTag != null) {
           value = new String(ch, start, length);
           if(value != null && !value.trim().equals("") && !value.trim().equals("\n")) {
              currentValue.append(value);
              String nowValue = currentValue.toString();
              if(currentTag.equals(WIRE_TYPE_TAG)){
                 endDevice.setWireMapType(wireMapType.get(nowValue));
             }else if(currentTag.equals(CONNECTS_TAG)) {
                String []connectsValue = nowValue.split(Connects_Value_Split_Regex);
                endDevice.setEndDeviceName(connectsValue[0]);
                endDevice.setEndPort(connectsValue[1]);
             }
           }
       }
   }
   
   @Override
   public void endElement(String uri, String localName, String name)
           throws SAXException {
       super.endElement(uri, localName, name);
      
       if (name.equals(nodeName)) {
          endDevices.add(endDevice);
          endDevice = null;
          currentTag = null;
       }
   }
   
}
