import { Component, OnInit } from '@angular/core';
import { ProvidersModule } from '../providers.module';
import {Router,ActivatedRoute} from '@angular/router';
import { DcimService } from '../../dcim/dcim.service';
@Component({
  selector: 'app-providers-add',
  templateUrl: './providers-add.component.html',
  styleUrls: ['./providers-add.component.scss']
})
export class ProvidersAddComponent implements OnInit {
  id:any;
  description:any;
  my_asset:ProvidersModule = new ProvidersModule();
  options:any;
  serverURL:any;
  loading:boolean = false;
  ignoreCertificatesModals:boolean = false;
  tip:string = "";
  operatingModals:boolean = false;
  arr=[ "Server", "PDU","Cabinet","Networks","Sensors","UPS"];
  subarr={
    "Server":["Blade","Standard"],
    "Sensors":["Humidity","Temperature","AirPressure","AirFlow","ContactClosure","Smoke","Water","Vibration"],
    "Cabinet":[],
    "Networks":[],
    "PDU":[],
    "UPS":[],
  }
  constructor(private service:DcimService,private router:Router,private activedRoute:ActivatedRoute) { }

  ngOnInit() {
  }
saveregion(region:string){
this.my_asset.region=region;
alert(this.my_asset.region);
}

savecountry(country:string){
  this.my_asset.country=country;
  alert(this.my_asset.country);

}

  cancle(){ this.router.navigate(["/ui/nav/facility/providers/providers-list"]);}
  save(){
    // alert(this.my_asset.category1);
    this.loading = true;
    this.service.Addmyasset(this.my_asset).subscribe(
      (data)=>{
        if(data.status == 201){
          // alert(data);
          this.loading = false;
          this.router.navigate(["/ui/nav/facility/providers/providers-list"]);
        }
      },
      error=>{  
        if(error.status == 400 && error.json().errors[0] == "Invalid SSL Certificate"){
          alert('400');
          this.loading = false;
          this.ignoreCertificatesModals = true;
          this.tip = error.json().message+". Are you sure you ignore the certificate check?";
          alert(this.tip);
        }else if(error.status == 400 && error.json().errors[0] == "Unknown Host"){
          alert('400');
          this.loading = false;
          this.operatingModals = true;
          this.tip = error.json().message+". Please check your serverIp. ";
        }else if(error.status == 401){
          alert('401');
          this.loading = false;
          this.operatingModals = true;
          this.tip = error.json().message+". Please check your userName or password. ";
        }else{
          this.loading = false;
          this.operatingModals = true;
          this.tip = error.json().message+". Please check your input. ";
        }
      }
    )

}




  cancel(){
    this.router.navigate(["/ui/nav/facility/providers/providers-list"]);
  }

}
