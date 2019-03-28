/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.poweriqworker.model;

import java.util.List;

public class RoomsResult {

   private List<Room> rooms;

   public List<Room> getRooms() {
      return rooms;
   }

   public void setRooms(List<Room> rooms) {
      this.rooms = rooms;
   }
   
   
}
