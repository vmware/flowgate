/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
import { Component, OnInit, ViewChild } from '@angular/core';
import { FacilityAdapterModule } from '../facility-adapter.module';
import { ClrWizard } from "@clr/angular";
import { AdapterJobCommandModule } from '../adapter-job-command.module';
import { FacilityAdapterService } from '../facility-adapter.service';

@Component({
  selector: 'app-adaptertype-list',
  templateUrl: './facility-adapter-list.component.html',
  styleUrls: ['./facility-adapter-list.component.scss']
})
export class FacilityAdapterListComponent implements OnInit {

  constructor(private facilityAdapterService:FacilityAdapterService ) { }

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

  setInfo(){
    this.getFacilityAdapters(this.currentPage,this.pageSize)
  }
  previous(){
    if(this.currentPage>1){
      this.currentPage--;
      this.getFacilityAdapters(this.currentPage,this.pageSize)
    }
  }
  next(){
    if(this.currentPage < this.totalPage){
      this.currentPage++
      this.getFacilityAdapters(this.currentPage,this.pageSize)
    }
  }
  getFacilityAdapters(currentPage,pageSize){
    this.facilityAdapterService.getAdapterByPagee(currentPage,pageSize).subscribe(
      (data)=>{
        if(data.status == 200){
            this.adapters = data.json().content;
            this.currentPage = data.json().number+1;
            this.totalPage = data.json().totalPages
            if(this.totalPage == 1){
              this.nextbtnDisabled = "disabled";
            }else{
              this.nextbtnDisabled = "";
            }  
        }
    })
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
    this.newCommand = new AdapterJobCommandModule();
  }

  addNewCommand(){
    this.addNewCommandSubmitLoading = true;
    if(this.checkCommandNameExisted(this.newCommand.command)){
      this.addCommandErrorClosed = false;
    }else{
      this.addCommandErrorClosed = true;
      this.predefineAdapterCommands.push(this.newCommand);
      this.newCommand = new AdapterJobCommandModule();
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
        if(data.status == 201){
          this.addLoadingFlag = false;
          this.wizard.forceFinish();
          this.wizard.reset();
          this.addErrorClosed = true;
          this.addFacilityAdapterOpen = false;
          this.getFacilityAdapters(this.currentPage,this.pageSize);
      }
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
    this.editCommand = new AdapterJobCommandModule();
    this.editCommand.description = command.description;
    this.editCommand.command = command.command;
    this.editCommand.triggerCycle = command.triggerCycle;
  }

  saveEditCommand(editcommand:AdapterJobCommandModule){
    this.editCommandSubmitLoading = true;
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
    this.editCommand = new AdapterJobCommandModule();
  }

  resetNewCommandForm(){
    this.onAdd = false;
    this.addCommandErrorClosed = true;
    this.newCommand = new AdapterJobCommandModule();
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

  onEditAdapter(adapter:FacilityAdapterModule){
    this.editFacilityAdapterOpen = true;
    this.editAdapter = adapter;
    this.prepareAdapterName(adapter);
  }

  prepareAdapterName(adapter:FacilityAdapterModule){
    this.commandsNameListForEditAdapter = [];
    adapter.commands.forEach(element => {
      this.commandsNameListForEditAdapter.push(element.command);
    });
  }

  openAddCommandForEditAdapter(){
    this.onAddForEditAdapter = true;
    this.oneditForEditAdapter = false;
    this.newCommandForEditAdapter = new AdapterJobCommandModule();
  }

  addNewCommandForEditAdapter(){
    this.addNewCommandSubmitLoadingForEditAdapter = true;
    if(this.checkCommandNameExistedForEditAdapter(this.newCommandForEditAdapter.command)){
      this.addCommandErrorClosedForEditAdapter = false;
    }else{
      this.addCommandErrorClosedForEditAdapter = true;
      this.editAdapter.commands.push(this.newCommandForEditAdapter);
      this.newCommandForEditAdapter = new AdapterJobCommandModule();
      this.onAddForEditAdapter = false;
      this.refreshNameListForEdit();
    }
    this.addNewCommandSubmitLoadingForEditAdapter = false;
  }
  refreshNameListForEdit(){
    this.commandsNameListForEditAdapter = [];
    this.editAdapter.commands.forEach(element => {
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
    this.editCommandForEditAdapter = new AdapterJobCommandModule();
  }

  saveEditCommandForEditAdapter(editcommand:AdapterJobCommandModule){
    this.editCommandSubmitLoadingForEditAdapter = true;
    this.editAdapter.commands.forEach(element => {
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
    this.newCommandForEditAdapter = new AdapterJobCommandModule();
  }

  onEditForEditAdapter(command:AdapterJobCommandModule){
    this.onAddForEditAdapter = false;;
    this.oneditForEditAdapter = true;
    this.editCommandForEditAdapter = new AdapterJobCommandModule();
    this.editCommandForEditAdapter.description = command.description;
    this.editCommandForEditAdapter.command = command.command;
    this.editCommandForEditAdapter.triggerCycle = command.triggerCycle;
  }

  onDeleteForEditAdapter(command:AdapterJobCommandModule){
    for (let index = 0; index < this.editAdapter.commands.length; index++) {
      const element = this.editAdapter.commands[index];
      if(element.command == command.command){
        this.editAdapter.commands.splice(index,1);
        this.onedit = false;
      }
    }
    this.refreshNameListForEdit();
  }

  editErrorClosed:boolean = true;
  editErrorMsg:string = "Internal error"
  loadingFlag:boolean = false;
  updateAdapter(){
    this.loadingFlag = true;
    this.facilityAdapterService.updateFacilityAdapter(this.editAdapter).subscribe(
      (data)=>{
        if(data.status == 200){
          this.loadingFlag = false;
          this.editwizard.forceFinish();
          this.editwizard.reset();
          this.editFacilityAdapterOpen = false;
          this.editErrorClosed = true;
          this.getFacilityAdapters(this.currentPage,this.pageSize);
      }
    },(error)=>{
      this.editErrorClosed = false;  
      this.editErrorMsg = error.json().message;
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
        if(data.status == 200){
          this.confirmDeleteShow = false;
          this.deleteErrorClosed = true;
          this.getFacilityAdapters(this.currentPage,this.pageSize);
      }
    },(error)=>{
      this.confirmDeleteShow = false;
      this.deleteErrorMsg = error.json().message;
      this.deleteErrorClosed = false;
      }
    )
  }


ngOnInit() {
  this.getFacilityAdapters(this.currentPage,this.pageSize);
}

}
