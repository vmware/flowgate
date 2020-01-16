import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClarityModule } from '@clr/angular';
import { ReactiveFormsModule,FormsModule } from '@angular/forms';
import { PagesModule } from '../../../pages.module';
import { ProvidersRoutingModule } from './providers-routing.module';
import { ProvidersComponent } from './providers.component';
import { ProvidersAddComponent } from './providers-add/providers-add.component';
import { DcimService } from '../dcim/dcim.service';
import{AssetStatusModule} from  './modules/asset-status/asset-status.module';
import{AssetRealtimeDataSpecModule} from  './modules/asset-realtime-data-spec/asset-realtime-data-spec.module';
@NgModule({
  imports: [
    CommonModule,
    ProvidersRoutingModule,
    ClarityModule,
    ReactiveFormsModule,
    FormsModule,
    PagesModule,
    AssetStatusModule,
    AssetRealtimeDataSpecModule,
   
  ],
  declarations: [ProvidersComponent, ProvidersAddComponent],

  providers:[DcimService]
})
//ProvidersModule is AssetModule
export class ProvidersModule {

  category:string;
  subCategory:string;
  assetNumber:any;
  assetName :string;
  assetSource:string;
  manufacturer:string;
  model : string;
  serialnumber : string;
  tag : string;
  region : string;
  country : string;
  city : string;
  building : string;
  floor : string
  room : string
  row : string
  col : string
  extraLocation : string;
  cabinetName : string;
  cabinetUnitPosition : number;
  mountingSide : string;
  cabinetsize : number;
  cabinetAssetNumber : string;
  created : number;
  assetRealtimeDataSpec : AssetRealtimeDataSpecModule=new AssetRealtimeDataSpecModule();
  status: AssetStatusModule =new AssetStatusModule();
 }
