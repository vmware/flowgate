import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { ProvidersComponent } from './providers.component';
import { ProvidersAddComponent } from './providers-add/providers-add.component';
import { ProvidersEditComponent } from './providers-edit/providers-edit.component';
import { ProvidersListComponent } from './providers-list/providers-list.component';
const routes: Routes = [

  {
    path: '',
    component: ProvidersComponent,
    children:[
      {path: '', redirectTo: 'providers-add', pathMatch: 'full'},
      {
      path:'providers-list',
      component:ProvidersListComponent
    },{
      path:'providers-edit/:id',
      component:ProvidersEditComponent
    },{
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
