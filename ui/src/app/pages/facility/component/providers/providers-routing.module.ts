import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { ProvidersComponent } from './providers.component';
import { ProvidersAddComponent } from './providers-add/providers-add.component';

const routes: Routes = [

  {
    path: '',
    component: ProvidersComponent,
    children:[
      {path: '', redirectTo: 'providers-add', pathMatch: 'full'},
    {
      path:'providers-add',
      component:ProvidersAddComponent
    }]
  }

];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProvidersRoutingModule { }
