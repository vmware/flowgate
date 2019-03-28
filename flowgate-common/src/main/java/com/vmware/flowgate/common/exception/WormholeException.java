/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.exception;


public final class WormholeException extends RuntimeException  {
   
   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public WormholeException(final String message) {
      super(message);
   }
   
   public WormholeException(final String message, final Throwable cause) {
      super(message, cause);
   }
}
