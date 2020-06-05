/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.exception;

import org.springframework.http.HttpStatus;


public final class WormholeRequestException extends RuntimeException {

   private static final long serialVersionUID = 1L;
   private HttpStatus status;
   private String errorCode;
   public static final String InvalidSSLCertificateCode="Invalid SSL Certificate";
   public static final String SSLCertificateRequiredCode = "SSL Certificate Required";
   public static final String UnknownHostCode  = "Unknown Host";
   public static final String DefaultErrorCode="Internal error";
   public HttpStatus getStatus() {
      return status;
   }

   public void setStatus(HttpStatus status) {
      this.status = status;
   }

   public String getErrorCode() {
      return errorCode;
   }

   public void setErrorCode(String errorCode) {
      this.errorCode = errorCode;
   }

   public WormholeRequestException() {
      super();
      this.errorCode=DefaultErrorCode;
      this.status = HttpStatus.BAD_REQUEST;
   }

   public WormholeRequestException(final String message, final Throwable cause) {
      super(message, cause);
      this.errorCode=DefaultErrorCode;
      this.status = HttpStatus.BAD_REQUEST;
   }

   public WormholeRequestException(final String message) {
      super(message);
      this.errorCode=DefaultErrorCode;
      this.status = HttpStatus.BAD_REQUEST;
   }

   public WormholeRequestException(final Throwable cause) {
      super(cause);
      this.errorCode=DefaultErrorCode;
      this.status = HttpStatus.BAD_REQUEST;
   }

   public WormholeRequestException(HttpStatus status, final String message, final Throwable cause) {
      super(message, cause);
      this.errorCode=DefaultErrorCode;
      this.status = status;

   }

   public WormholeRequestException(HttpStatus status, final String message, final Throwable cause, String errorCode) {
      super(message, cause);
      this.errorCode=errorCode;
      this.status = status;

   }

   public static WormholeRequestException InvalidFiled(String field, String value) {
      String message = String.format("Invalid value: '%s' for field: '%s' ", value, field);
      return new WormholeRequestException(message);
   }

   public static WormholeRequestException NotFound(String item, String queryParam, String value) {
      String message =  String.format("Failed to find %s with field: %s  and value: %s", item, queryParam, value);
      return new WormholeRequestException(HttpStatus.NOT_FOUND, message, null);
   }

}
