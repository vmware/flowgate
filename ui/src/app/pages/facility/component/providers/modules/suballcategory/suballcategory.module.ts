import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
@NgModule({
  imports: [
    CommonModule
  ],
  declarations: []
})
export class SuballcategoryModule { 
    Server=["Blade","Standard"];
    Sensors=["Humidity","Temperature","AirPressure","AirFlow","ContactClosure","Smoke","Water","Vibration"];
    Cabinet=[];
    Networks=[];
    PDU=[];
    UPS=[];
}
