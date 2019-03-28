/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.management.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class WormholeExceptionHander extends ResponseEntityExceptionHandler {

   @ExceptionHandler({ WormholeRequestException.class })
   public ResponseEntity<Object> handWormholeException(final WormholeRequestException ex,
         final WebRequest request) {
      logger.info(ex.getClass().getName());
      logger.error("error", ex);

      final ApiError apiError = new ApiError(ex.getStatus(), "Internal error", ex.getMessage());

      return new ResponseEntity<Object>(apiError, new HttpHeaders(), apiError.getStatus());
   }

   //500
   @ExceptionHandler({ Exception.class })
   public ResponseEntity<Object> handleAll(final Exception ex, final WebRequest request) {
      logger.info(ex.getClass().getName());
      logger.error("error", ex);
      //
      final ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getLocalizedMessage(), "error occurred");
      return new ResponseEntity<Object>(apiError, new HttpHeaders(), apiError.getStatus());
   }
}
