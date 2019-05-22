/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.common.model;

import java.io.Serializable;
import java.util.List;

public class PageModelImp<T> implements Serializable{

   private static final long serialVersionUID = 1L;
   private List<T> content;
   private int totalPages;
   private int totalElements;
   private int number;
   private int size;
   private int numberOfElements;
   private boolean last;
   private boolean first;
   public List<T> getContent() {
      return content;
   }
   public void setContent(List<T> content) {
      this.content = content;
   }
   public int getTotalPages() {
      return totalPages;
   }
   public void setTotalPages(int totalPages) {
      this.totalPages = totalPages;
   }
   public int getTotalElements() {
      return totalElements;
   }
   public void setTotalElements(int totalElements) {
      this.totalElements = totalElements;
   }
   public int getNumber() {
      return number;
   }
   public void setNumber(int number) {
      this.number = number;
   }
   public int getSize() {
      return size;
   }
   public void setSize(int size) {
      this.size = size;
   }
   public int getNumberOfElements() {
      return numberOfElements;
   }
   public void setNumberOfElements(int numberOfElements) {
      this.numberOfElements = numberOfElements;
   }
   public boolean isLast() {
      return last;
   }
   public void setLast(boolean last) {
      this.last = last;
   }
   public boolean isFirst() {
      return first;
   }
   public void setFirst(boolean first) {
      this.first = first;
   }

}
