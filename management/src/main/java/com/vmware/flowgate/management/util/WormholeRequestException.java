/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.management.util;

import org.springframework.http.HttpStatus;

public final class WormholeRequestException extends RuntimeException {
   private HttpStatus status;

   public HttpStatus getStatus() {
      return status;
   }

   public void setStatus(HttpStatus status) {
      this.status = status;
   }

   public WormholeRequestException() {
      super();
      this.status = HttpStatus.BAD_REQUEST;
   }

   public WormholeRequestException(final String message, final Throwable cause) {
      super(message, cause);
      this.status = HttpStatus.BAD_REQUEST;
   }

   public WormholeRequestException(final String message) {
      super(message);
      this.status = HttpStatus.BAD_REQUEST;
   }

   public static WormholeRequestException ServerAlreadyExsit(final String server) {
      String message = String.format("The server %s is already exsit.", server);
      return new WormholeRequestException(message);
   }

   public static WormholeRequestException InvalidFiled(String field, String value) {
      String message = String.format("Invalid value: '%s' for field: '%s' ", value, field);
      return new WormholeRequestException(message);
   }

   public WormholeRequestException(final Throwable cause) {
      super(cause);
      this.status = HttpStatus.BAD_REQUEST;
   }

   public WormholeRequestException(HttpStatus status, final String message, final Throwable cause) {
      super(message, cause);
      this.status = status;

   }
}
