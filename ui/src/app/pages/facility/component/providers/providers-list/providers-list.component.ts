import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-providers-list',
  templateUrl: './providers-list.component.html',
  styleUrls: ['./providers-list.component.scss']
})
export class ProvidersListComponent implements OnInit {
  

  id:string='';
  options:any='';
  assetNumber:string='';

  constructor() { }

  ngOnInit() {
  }

}
