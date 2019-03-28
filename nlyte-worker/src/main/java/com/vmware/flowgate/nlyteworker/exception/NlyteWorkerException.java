/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.nlyteworker.exception;


public final class NlyteWorkerException extends RuntimeException {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public NlyteWorkerException() {
      super();
   }

   public NlyteWorkerException(final String message, final Throwable cause) {
      super(message, cause);
   }

   public NlyteWorkerException(final String message) {
      super(message);
   }

   public NlyteWorkerException(final Throwable cause) {
      super(cause);
   }

}
