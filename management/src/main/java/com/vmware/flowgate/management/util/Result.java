/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.management.util;

/**
 * Result
 */
public class Result<T> {

   public static final int ERRCODE_SUCCESS = 1;
   public static final int ERRCODE_FAIL = 0;

   private int code;
   private String message;
   private T data;

   public Result(int code, String message, T data) {
      super();
      this.code = code;
      this.message = message;
      this.data = data;
   }

   public Result(int code, String message) {
      super();
      this.code = code;
      this.message = message;
   }

   public Result() {
   }

   public int getCode() {
      return code;
   }

   public void setCode(int code) {
      this.code = code;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public T getData() {
      return data;
   }

   public void setData(T data) {
      this.data = data;
   }


   /***
    * Create an empty successful return object with no data
    *
    * @param <T>
    * @return
    */
   public static <T> Result<T> createOK() {
      Result<T> ok = new Result<>(ERRCODE_SUCCESS, "");
      return ok;
   }

   /***
    * Create an empty failed return object with no data
    *
    * @param <T>
    * @return
    */
   public static <T> Result<T> createFail() {
      Result<T> fail = new Result<>(ERRCODE_FAIL, "");
      return fail;
   }

   /***
    * Create a failed data returned object with prompt information.
    *
    * @param <T>
    * @return
    */
   public static <T> Result<T> createFail(String message) {
      Result<T> fail = new Result<>(ERRCODE_FAIL, "");
      fail.setMessage(message);
      return fail;
   }

   /**
    * Based on the returned data, we create a successful return object.
    *
    * @param data
    * @param <T>
    * @return
    */
   public static <T> Result<T> createOK(T data) {
      Result<T> ok = createOK();
      ok.setData(data);
      return ok;
   }

}
