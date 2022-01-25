import { Component, OnInit } from '@angular/core';
import {SelectItem} from 'primeng/api';
import {Dataset, DatasetProperty} from "../json/Dataset";
import {ApplicationStateService, UpdateSpec} from "../application-state.service";
import {AdminService} from "../../admin.service";
import {Subscription} from "rxjs";

@Component({
  selector: 'app-dataset-update',
  templateUrl: './dataset-update.component.html',
  styleUrls: ['./dataset-update.component.css']
})
export class DatasetUpdateComponent implements OnInit {

  stateChanges: Subscription;
  header = "Select a Data Set for Automatic Updates";
  subHeader = "Select the edit button to set up automatic updates for this data set.";
  update_dataset: Dataset;
  update_property: DatasetProperty;
  cron_spec: string;
  edit_dataset_update: boolean = false;
  dataset_title: string;
  update_unit: string;
  minutes: string;
  hours: string;
  constructor(private applicationStateService: ApplicationStateService,
              private adminService: AdminService) { }

  ngOnInit(): void {

    this.stateChanges = this.applicationStateService.stateChanged.subscribe(state => {
      if (state) {
        this.update_dataset = state.dataset_to_edit_updates;
        if ( this.update_dataset ) {
          this.edit_dataset_update = true;
          this.dataset_title = this.update_dataset.title;
          if (this.update_dataset.datasetProperties) {
            for (let i = 0; i < this.update_dataset.datasetProperties.length; i++) {
              let property: DatasetProperty = this.update_dataset.datasetProperties[i];
              if ( property.type == 'update') {
                this.cron_spec = property.value;
                this.update_property = property;
              }
            }
          }
        }
      }
    });

  }
  close() {
    this.applicationStateService.reinit();
    this.edit_dataset_update = false;
    this.cron_spec = null;
    this.update_dataset = null;
  }
  save() {
    let update: UpdateSpec = new class implements UpdateSpec {
      cron_spec: string;
      datset: any;
    };
    this.applicationStateService.setForRequest();
    update["cron_spec"] = this.cron_spec;
    update["dataset"] = this.update_dataset.id;
    const parent_type = this.applicationStateService.getParentType('parent_type');
    this.adminService.saveDatasetUpdateSpec(update).subscribe(data => {
        this.applicationStateService.setParent(data, parent_type, false);
        // In this case wait for the return to happen before closing
        this.applicationStateService.reinit();
        this.edit_dataset_update = false;
        this.cron_spec = null;
        this.update_dataset = null;
      },
      error => {
        if ( error.message.includes("auth/login") ) {
          this.applicationStateService.setError("Failed to save changes. Are you logged in as admin. Reload the page to login.");
        } else {
          this.applicationStateService.setError("Failed to save changes. Is the server running.");
        }
      });
  }
}
