import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import {ReactiveFormsModule} from "@angular/forms";
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { JsonFormComponent } from './forms/json-form/json-form.component';
import { FormPropertyComponent } from './forms/form-property/form-property.component';
import { DatasetPickerComponent } from './dataset-picker/dataset-picker.component';
import {AdminComponent} from "./admin/admin.component";
import { DatasetEditComponent } from './dataset-edit/dataset-edit.component';
import { DatasetAddComponent } from './dataset-add/dataset-add.component';
import { DatasetOrganizeComponent } from './dataset-organize/dataset-organize.component';
import { DatasetHideComponent } from './dataset-hide/dataset-hide.component';
import { DatasetDeleteComponent } from './dataset-delete/dataset-delete.component';
import { SiteEditComponent } from './site-edit/site-edit.component';
import { DatasetUpdateComponent } from './dataset-update/dataset-update.component';
import { BackupRestoreComponent } from './backup-restore/backup-restore.component';
import {DatasetService} from "../dataset.service";
import {AdminService} from "../admin.service";
import {ApplicationStateService} from "./application-state.service";
import {JsonFormService} from "../json-form.service";
import {ProgressBarModule} from "primeng/progressbar";
import {TabViewModule} from "primeng/tabview";
import {DialogModule} from "primeng/dialog";
import {CardModule} from "primeng/card";
import {ScrollPanelModule} from "primeng/scrollpanel";
import {ButtonModule} from "primeng/button";
import {BreadcrumbModule} from 'primeng/breadcrumb';
import {MenuItem} from 'primeng/api';
import {RadioButtonModule} from "primeng/radiobutton";
import {CheckboxModule} from "primeng/checkbox";

@NgModule({
    declarations: [
        AppComponent,
        JsonFormComponent,
        FormPropertyComponent,
        DatasetPickerComponent,
        AdminComponent,
        DatasetEditComponent,
        DatasetAddComponent,
        DatasetOrganizeComponent,
        DatasetHideComponent,
        DatasetDeleteComponent,
        SiteEditComponent,
        DatasetUpdateComponent,
        BackupRestoreComponent,
    ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpClientModule,
    ProgressBarModule,
    TabViewModule,
    DialogModule,
    CardModule,
    ScrollPanelModule,
    ButtonModule,
    BreadcrumbModule,
    RadioButtonModule,
    CheckboxModule
  ],
    providers: [
        DatasetService,
        AdminService,
        ApplicationStateService,
        JsonFormService
    ],
    bootstrap: [AppComponent]
})
export class AppModule { }
