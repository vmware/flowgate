import { Component, OnInit } from '@angular/core';
import { ProvidersModule } from '../providers.module';
import {Router,ActivatedRoute} from '@angular/router';
import { DcimService } from '../../dcim/dcim.service';
import { error } from 'util';
import{SuballcategoryModule} from '../modules/suballcategory/suballcategory.module';
@Component({
  selector: 'app-providers-add',
  templateUrl: './providers-add.component.html',
  styleUrls: ['./providers-add.component.scss']
})
export class ProvidersAddComponent implements OnInit {
  description:any;
  my_asset:ProvidersModule = new ProvidersModule();
  suballcategory:SuballcategoryModule =new SuballcategoryModule();
  addsuccess:boolean = false;
  addfail:boolean =false;
  tip:string = " ";
  allcategory=[ "Server", "PDU","Cabinet","Networks","Sensors","UPS"];
  constructor(private service:DcimService,private router:Router,private activedRoute:ActivatedRoute) { }
  ngOnInit() {
  }
  closesuccessTips(){ 
    this.addsuccess=false;
  }
  closefailTips(){
    this.addfail=false;
  }
  cancel(){ this.router.navigate(["/ui/nav/facility"]);}
  save(){
    this.service.Addmyasset(this.my_asset).subscribe(
      (data)=>{
        if(data.status == 201){
          this.addsuccess=true;
        }
      },
      error=>{  
        if(error.status == 400)
        {
          this.addfail =true;
          this.tip = "Status:"+ error.status+ ",Please try to input again";
        }
        else{
          this.addfail =true;
          this.tip = "Status:"+ error.status+ ",Please try again later";
        }
              
      }
    )
    }


  }




  


