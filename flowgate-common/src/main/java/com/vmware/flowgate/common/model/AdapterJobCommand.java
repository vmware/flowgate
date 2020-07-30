/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

public class AdapterJobCommand {
   private String command;
   private String description;
   private int triggerCycle;

   public String getCommand() {
      return command;
   }

   public void setCommand(String command) {
      this.command = command;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public int getTriggerCycle() {
      return triggerCycle;
   }

   public void setTriggerCycle(int triggerCycle) {
      this.triggerCycle = triggerCycle;
   }

}
