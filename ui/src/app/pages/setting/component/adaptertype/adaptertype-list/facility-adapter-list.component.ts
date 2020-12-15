/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit, ViewChild } from '@angular/core';
import { FacilityAdapterModule } from '../facility-adapter.module';
import { ClrDatagridStateInterface, ClrWizard } from "@clr/angular";
import { AdapterJobCommandModule } from '../adapter-job-command.module';
import { FacilityAdapterService } from '../facility-adapter.service';
import { FormBuilder, FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

function triggerCycleMatchValidator(triggerCycle: string): ValidatorFn {
  return (control: FormControl) => {
    if (!control || !control.parent) {
      return null;
    }
    if(control.parent.get(triggerCycle).value != 0 && control.parent.get(triggerCycle).value % 5 == 0){
      return null;
    }
    return { mismatch: true };
  };
}

@Component({
  selector: 'app-adaptertype-list',
  templateUrl: './facility-adapter-list.component.html',
  styleUrls: ['./facility-adapter-list.component.scss']
})
export class FacilityAdapterListComponent implements OnInit {

  formPageOne:FormGroup;
  editformPageOne:FormGroup;
  commandForm:FormGroup;
  commandEditForm:FormGroup;
  editAdapterCommandForm:FormGroup;
  editCommandFormForEditAdapter:FormGroup;

  alertclose:boolean = true;
  alertType:string = "";
  alertcontent:string = "";

  constructor(private facilityAdapterService:FacilityAdapterService ,private fb: FormBuilder) {
    this.formPageOne = this.fb.group({
      type: ['', [
        Validators.required
      ]],
      displayName: ['', [
        Validators.required,
        Validators.pattern(/^[a-zA-Z][a-zA-Z0-9_]*$/)
      ]],
      description: ['', [
      ]]
    });
    this.editformPageOne = this.fb.group({
      type: ['', [
        Validators.required
      ]],
      displayName: ['', [
        Validators.required,
        Validators.pattern(/^[a-zA-Z][a-zA-Z0-9_]*$/)
      ]],
      description: ['', [
      ]],
      id: ['', [
      ]]
    });
    this.commandForm = this.fb.group({
      command: ['', [
        Validators.required,
        Validators.pattern(/^[a-zA-Z][a-zA-Z0-9_]*$/)
      ]],
      triggerCycle: ['', [
        Validators.required,
        Validators.pattern(/^[0-9]*[1-9][0-9]*$/),
        triggerCycleMatchValidator('triggerCycle')
      ]],
      description: ['', [
      ]]
    });
    this.commandEditForm = this.fb.group({
      command: ['', [
        Validators.required,
        Validators.pattern(/^[a-zA-Z][a-zA-Z0-9_]*$/)
      ]],
      triggerCycle: ['', [
        Validators.required,
        Validators.pattern(/^[0-9]*[1-9][0-9]*$/),
        triggerCycleMatchValidator('triggerCycle')
      ]],
      description: ['', [
      ]]
    });
    this.editAdapterCommandForm = this.fb.group({
      command: ['', [
        Validators.required,
        Validators.pattern(/^[a-zA-Z][a-zA-Z0-9_]*$/)
      ]],
      triggerCycle: ['', [
        Validators.required,
        Validators.pattern(/^[0-9]*[1-9][0-9]*$/),
        triggerCycleMatchValidator('triggerCycle')
      ]],
      description: ['', [
      ]]
    });
    this.editCommandFormForEditAdapter = this.fb.group({
      command: ['', [
        Validators.required,
        Validators.pattern(/^[a-zA-Z][a-zA-Z0-9_]*$/)
      ]],
      triggerCycle: ['', [
        Validators.required,
        Validators.pattern(/^[0-9]*[1-9][0-9]*$/),
        triggerCycleMatchValidator('triggerCycle')
      ]],
      description: ['', [
      ]]
    });
  }

  //alert
  Duplicate_Command_Name:string = "Duplicate command name.";
  Trigger_Cycle_Number:string = "The number should be multiples of 5.";
  Command_Name:string = "Please use letter and number combination for the adapter name, it must start with a letter.";
  Nem_Command_Form_Lable:string = "New Command";
  Add_Command:string = "Add Command";
  Command_Description:string = "Flowgate will trigger the commands list follow for each adapter with the specified trigger cycle. The syncmetadata command is planed to sync the asset metadata information. The syncmetricsdata command is planed to sync the metrics data. You can always add more commands when needed.";
  Delete_Info:string = "Please double check if you need to delete this adapter. This action is permanent and CANNOT be recovered.";
  Edit_adapter_command = "Edit adapter command";
  @ViewChild("addFacilityAdapterwizard") wizard: ClrWizard;
  addFacilityAdapterOpen:boolean = false;
  newFacilityAdapter:FacilityAdapterModule = new FacilityAdapterModule()
  adapters:FacilityAdapterModule[] = [];
  predefineAdapterCommands:AdapterJobCommandModule[] = [];
  predefineName:string[] = ["Nlyte","PowerIQ","Device42","InfoBlox","Labsdb"];
  newCommand:AdapterJobCommandModule = new AdapterJobCommandModule();
  editCommand:AdapterJobCommandModule = new AdapterJobCommandModule();
  addNewCommandSubmitLoading:boolean = false;
  editCommandSubmitLoading:boolean = false;
  onedit:boolean = false;
  onAdd:boolean = false;
  addCommandErrorClosed:boolean = true;
  commandsNameList:string[] = [];

  currentPage:number = 1;
  totalPage:number = 1;
  pageSize:string = '5';
  nextbtnDisabled:string = "disabled";

  showDetailPageShow:boolean = false;
  showDetaileAdapter:FacilityAdapterModule = new FacilityAdapterModule();

  showDetail(adapter:FacilityAdapterModule){
    this.showDetaileAdapter = adapter;
    this.showDetailPageShow = true;
  }

  translateTypeName(type:string):string{
    if(type == "OtherDCIM"){
      return "DCIM"
    }
    return "CMDB"
  }

  close(){
    this.alertclose = true;
  }

  loading:boolean = true;
  currentState:ClrDatagridStateInterface;
  totalItems:number = 0;
  refresh(state: ClrDatagridStateInterface){
    this.adapters = [];
    if (!state.page) {
      return;
    }
    this.currentState = state;
    this.getFacilityAdapters(state.page.current,state.page.size);
  }
  getFacilityAdapters(currentPage:number,pageSize:number){
    this.loading = true;
    this.facilityAdapterService.getAdapterByPagee(currentPage,pageSize).subscribe(
      (data)=>{
          this.adapters = data['content'];
          this.totalItems = data['totalElements'];
          this.loading = false;
    },(error)=>{
            this.loading = false;
            this.alertType = "danger";
            this.alertcontent = "Internal error";
            if(error._body != null && error.status != "0"){
                this.alertcontent = error.json().message;
            }
            this.alertclose = false;
        })
  }
  baseSetting(){
    this.newFacilityAdapter = this.formPageOne.value;
  }

  addAdapterType(){
    this.addFacilityAdapterOpen = true;
    this.commandsNameList = [];
    this.predefineAdapterCommands = [];
    let syncMetaDataJobCommand:AdapterJobCommandModule = new AdapterJobCommandModule();
    syncMetaDataJobCommand.command = "syncmetadata";
    syncMetaDataJobCommand.description = "Command for sync metadata job";
    syncMetaDataJobCommand.triggerCycle = 1440;
    this.predefineAdapterCommands.push(syncMetaDataJobCommand);
    let syncMetricsataJobCommand:AdapterJobCommandModule = new AdapterJobCommandModule();
    syncMetricsataJobCommand.command = "syncmetricsdata";
    syncMetricsataJobCommand.description = "Command for sync metrics data job";
    syncMetricsataJobCommand.triggerCycle = 5;
    this.predefineAdapterCommands.push(syncMetricsataJobCommand);
    this.commandsNameList.push("syncmetadata");
    this.commandsNameList.push("syncmetricsdata");
  }

  openAddCommand(){
    this.onAdd = true;
    this.onedit = false;
    this.commandForm.reset();
  }

  addNewCommand(){
    this.addNewCommandSubmitLoading = true;
    let newCommand:AdapterJobCommandModule = this.commandForm.value;
    if(this.checkCommandNameExisted(newCommand.command)){
      this.addCommandErrorClosed = false;
    }else{
      this.addCommandErrorClosed = true;
      this.predefineAdapterCommands.push(newCommand);
      this.commandForm.reset();
      this.onAdd = false;
      this.refreshNameList();
    }
    this.addNewCommandSubmitLoading = false;
  }

  refreshNameList(){
    this.commandsNameList = [];
    this.predefineAdapterCommands.forEach(element => {
      this.commandsNameList.push(element.command);
    });
  }

  addErrorClosed:boolean = true;
  addErrorMsg:string = "Interal error"
  addLoadingFlag:boolean = false;
  saveNewAdapter(){
    this.newFacilityAdapter.commands = this.predefineAdapterCommands;
    this.facilityAdapterService.createFacilityAdapter(this.newFacilityAdapter).subscribe(
      (data)=>{
        this.addLoadingFlag = false;
        this.wizard.forceFinish();
        this.wizard.reset();
        this.formPageOne.reset();
        this.addErrorClosed = true;
        this.addFacilityAdapterOpen = false;
        this.refresh(this.currentState);
    },(error) =>{
        this.addErrorClosed = false;
        this.addErrorMsg = error.json().message;
      }
    )
  }

  addAdapterDoCancel(): void {
      this.wizard.close();
      this.wizard.reset();
      this.newFacilityAdapter = new FacilityAdapterModule();
  }

  addAdapterGoBack(): void {
    this.wizard.previous();
  }

  onEdit(command:AdapterJobCommandModule){
    this.onAdd = false;;
    this.onedit = true;
    this.commandEditForm.setValue(command);
  }

  saveEditCommand(){
    this.editCommandSubmitLoading = true;
    let editcommand:AdapterJobCommandModule = this.commandEditForm.value;
    this.predefineAdapterCommands.forEach(element => {
      if(element.command == editcommand.command){
        element.triggerCycle = editcommand.triggerCycle;
        element.description = editcommand.description;
      }
    });
    this.editCommandSubmitLoading = false;
    this.onedit = false;
  }

  checkCommandNameExisted(name:string):boolean{
    if(this.commandsNameList.indexOf(name) != -1){
      return true;
    }
    return false;
  }

  resetEditCommandForm(){
    this.onedit = false;
    this.commandEditForm.reset();
  }

  resetNewCommandForm(){
    this.onAdd = false;
    this.addCommandErrorClosed = true;
    this.commandForm.reset();
  }

  onDelete(command:AdapterJobCommandModule){
    for (let index = 0; index < this.predefineAdapterCommands.length; index++) {
      const element = this.predefineAdapterCommands[index];
      if(element.command == command.command){
        this.predefineAdapterCommands.splice(index,1);
        this.onedit = false;
      }
    }
    this.refreshNameList();
  }

  validTriggerCyle:boolean = false;
  getValidationState(){
    return this.validTriggerCyle;
  }

  handleValidation(value:number): void {
   if(value != 0 && value % 5 == 0){
     this.validTriggerCyle = false;
   }else{
     this.validTriggerCyle = true;
   }
  }

  checkIsPredefineData(command:AdapterJobCommandModule):boolean{
    if(command.command == 'syncmetadata' || command.command == 'syncmetricsdata' ){
      return true;
    }
    return false;
  }

  editAdapter:FacilityAdapterModule = new FacilityAdapterModule();
  @ViewChild("editFacilityAdapterwizard") editwizard: ClrWizard;
  editFacilityAdapterOpen:boolean = false;

  newCommandForEditAdapter:AdapterJobCommandModule = new AdapterJobCommandModule();
  editCommandForEditAdapter:AdapterJobCommandModule = new AdapterJobCommandModule();
  addNewCommandSubmitLoadingForEditAdapter:boolean = false;
  editCommandSubmitLoadingForEditAdapter:boolean = false;
  oneditForEditAdapter:boolean = false;
  onAddForEditAdapter:boolean = false;
  addCommandErrorClosedForEditAdapter:boolean = true;
  commandsNameListForEditAdapter:string[] = [];
  predefineAdapterCommandsForEditAdapter:AdapterJobCommandModule[] = [];

  onEditAdapter(adapter:FacilityAdapterModule){
    this.editFacilityAdapterOpen = true;
    this.editformPageOne.get('type').setValue(adapter.type);
    this.editformPageOne.get('displayName').setValue(adapter.displayName);
    this.editformPageOne.get('description').setValue(adapter.description);
    this.editformPageOne.get('id').setValue(adapter.id);
    this.predefineAdapterCommandsForEditAdapter = adapter.commands;
    this.prepareAdapterName(adapter);
  }

  prepareAdapterName(adapter:FacilityAdapterModule){
    this.commandsNameListForEditAdapter = [];
    this.predefineAdapterCommandsForEditAdapter.forEach(element => {
      this.commandsNameListForEditAdapter.push(element.command);
    });
  }

  openAddCommandForEditAdapter(){
    this.onAddForEditAdapter = true;
    this.oneditForEditAdapter = false;
    //this.newCommandForEditAdapter = new AdapterJobCommandModule();
    this.editAdapterCommandForm.reset();
  }

  addNewCommandForEditAdapter(){
    this.addNewCommandSubmitLoadingForEditAdapter = true;
    let newCommand:AdapterJobCommandModule = this.editAdapterCommandForm.value;
    if(this.checkCommandNameExistedForEditAdapter(newCommand.command)){
      this.addCommandErrorClosedForEditAdapter = false;
    }else{
      this.addCommandErrorClosedForEditAdapter = true;
      this.predefineAdapterCommandsForEditAdapter.push(newCommand);
      this.newCommandForEditAdapter = new AdapterJobCommandModule();
      this.onAddForEditAdapter = false;
      this.refreshNameListForEdit();
    }
    this.addNewCommandSubmitLoadingForEditAdapter = false;
  }
  refreshNameListForEdit(){
    this.commandsNameListForEditAdapter = [];
    this.predefineAdapterCommandsForEditAdapter.forEach(element => {
      this.commandsNameListForEditAdapter.push(element.command);
    });
  }

  checkCommandNameExistedForEditAdapter(name:string):boolean{
    if(this.commandsNameListForEditAdapter.indexOf(name) != -1){
      return true;
    }
    return false;
  }

  resetEditCommandFormForEditAdapter(){
    this.oneditForEditAdapter = false;
    this.editCommandFormForEditAdapter.reset();
  }

  saveEditCommandForEditAdapter(){
    this.editCommandSubmitLoadingForEditAdapter = true;
    let editcommand:AdapterJobCommandModule = this.editCommandFormForEditAdapter.value;
    this.predefineAdapterCommandsForEditAdapter.forEach(element => {
      if(element.command == editcommand.command){
        element.triggerCycle = editcommand.triggerCycle;
        element.description = editcommand.description;
      }
    });
    this.editCommandSubmitLoadingForEditAdapter = false;
    this.oneditForEditAdapter = false;
  }

  resetNewCommandFormForEditAdapter(){
    this.onAddForEditAdapter = false;
    this.addCommandErrorClosedForEditAdapter = true;
    this.editAdapterCommandForm.reset();
  }

  onEditForEditAdapter(command:AdapterJobCommandModule){
    this.onAddForEditAdapter = false;;
    this.oneditForEditAdapter = true;
    this.editCommandFormForEditAdapter.setValue(command);
  }

  onDeleteForEditAdapter(command:AdapterJobCommandModule){
    for (let index = 0; index < this.predefineAdapterCommandsForEditAdapter.length; index++) {
      const element = this.predefineAdapterCommandsForEditAdapter[index];
      if(element.command == command.command){
        this.predefineAdapterCommandsForEditAdapter.splice(index,1);
        this.onedit = false;
      }
    }
    this.refreshNameListForEdit();
  }

  reinitadapterValue(){
    this.editAdapter = this.editformPageOne.value;
  }

  editErrorClosed:boolean = true;
  editErrorMsg:string = "Internal error"
  loadingFlag:boolean = false;
  updateAdapter(){
    this.loadingFlag = true;
    this.editAdapter = this.editformPageOne.value;
    this.editAdapter.commands = this.predefineAdapterCommandsForEditAdapter;
    this.facilityAdapterService.updateFacilityAdapter(this.editAdapter).subscribe(
      (data)=>{
        this.loadingFlag = false;
        this.editwizard.forceFinish();
        this.editwizard.reset();
        this.editFacilityAdapterOpen = false;
        this.editErrorClosed = true;
        this.refresh(this.currentState);
    },(error:HttpErrorResponse)=>{
        this.editErrorClosed = false;
        this.editErrorMsg = error.error.message;
        this.loadingFlag = false;
      }
    )
  }

doCancel(): void {
    this.editwizard.close();
    this.editwizard.reset();
    this.editAdapter = new FacilityAdapterModule();
}

goBack(): void {
  this.editwizard.previous();
}

removeAdapterId:string = "";
confirmDeleteShow:boolean = false;
deleteErrorClosed:boolean = true;
deleteErrorMsg:string = "Internal error";
deleteAdapter(id:string){
  this.removeAdapterId = id;
  this.confirmDeleteShow = true;
}
cancelDelete(){
  this.removeAdapterId = "";
  this.confirmDeleteShow = false;
}

comfirmDelete(){
  this.facilityAdapterService.deleteAdapterById(this.removeAdapterId).subscribe(
      (data)=>{
        this.confirmDeleteShow = false;
        this.deleteErrorClosed = true;
        this.refresh(this.currentState);

    },(error:HttpErrorResponse)=>{
        this.confirmDeleteShow = false;
        this.deleteErrorMsg = error.error.message;
        this.deleteErrorClosed = false;
      }
    )
  }

ngOnInit() {
}

}
