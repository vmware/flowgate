import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClarityModule } from '@clr/angular';
import { ReactiveFormsModule,FormsModule } from '@angular/forms';
import { PagesModule } from '../../../pages.module';
import { ProvidersRoutingModule } from './providers-routing.module';
import { ProvidersComponent } from './providers.component';
import { ProvidersAddComponent } from './providers-add/providers-add.component';
import { DcimService } from '../dcim/dcim.service';
@NgModule({
  imports: [
    CommonModule,
    ProvidersRoutingModule,
    ClarityModule,
    ReactiveFormsModule,
    FormsModule,
    PagesModule,
   
  ],
  declarations: [ProvidersComponent, ProvidersAddComponent],

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
  
  lastupdate : number;
  created : number;
  
   status={
    'status':"",
    'pduMapping':0,
    'networkMapping':0,
   }

 }
