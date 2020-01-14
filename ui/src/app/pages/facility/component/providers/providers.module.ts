import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClarityModule } from '@clr/angular';
import { ReactiveFormsModule,FormsModule } from '@angular/forms';
// import { BrowserModule } from '@angular/platform-browser';
// import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { PagesModule } from '../../../pages.module';
import { ProvidersRoutingModule } from './providers-routing.module';
import { ProvidersComponent } from './providers.component';
import { ProvidersAddComponent } from './providers-add/providers-add.component';
import { ProvidersEditComponent } from './providers-edit/providers-edit.component';
import { ProvidersListComponent } from './providers-list/providers-list.component';
import { DcimService } from '../dcim/dcim.service';
@NgModule({
  imports: [
    CommonModule,
    ProvidersRoutingModule,
    ClarityModule,
    ReactiveFormsModule,
    FormsModule,
    PagesModule,
    // BrowserModule,
    // BrowserAnimationsModule,
  ],
  declarations: [ProvidersComponent, ProvidersAddComponent, ProvidersEditComponent, ProvidersListComponent],

  providers:[DcimService]
})
export class ProvidersModule {
  
  public category1:string;
  subCategory:string;
  id:string='99';
  assetNumber:any;
  assetName :string;
  assetSource:string;
  manufacturer:string;
  model : String;
  serialnumber : String;
  tag : String;
  //  assetAddress : AssetAddress;
  region : String;
  country : String;
  city : String;
  building : String;
  floor : String
  room : String
  row : String
  col : String
  extraLocation : String;
  cabinetName : String;
  cabinetUnitPosition : number;
  mountingSide : string;
  cabinetsize : number;
  cabinetAssetNumber : String;
  // assetRealtimeDataSpec : AssetRealtimeDataSpec;
  // Justificationfields : HashMap<String, String>;
  // sensorsformulars : Map<ServerSensorType, String>;
  lastupdate : number;
  created : number;
  // pdus : List<String>;
  // switches : List<String>;
  // status : AssetStatus;
   status={
    'status':"",
    'pduMapping':0,
    'networkMapping':0,
   }

 }
