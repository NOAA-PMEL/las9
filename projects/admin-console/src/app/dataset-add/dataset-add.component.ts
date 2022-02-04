import {Component, OnInit, ViewChild} from '@angular/core';
import {DatasetPickerComponent} from "../dataset-picker/dataset-picker.component";
import {StringProperty} from "../forms/string-property";
import {FormControl, Validators} from "@angular/forms";
import {AddProperty} from "../../add-property";
import {AddRequest} from "../../add-request";
import {AdminService} from "../../admin.service";
import {ApplicationStateService} from "../application-state.service";

@Component({
  selector: 'app-dataset-add',
  templateUrl: './dataset-add.component.html',
  styleUrls: ['./dataset-add.component.css']
})
export class DatasetAddComponent implements OnInit {

  error:boolean = false;
  use_source_url:boolean = false;

  griddedSingleFormControl = new FormControl('', [
    Validators.required,
  ]);
  dsgSingleFormControl = new FormControl('', [
    Validators.required,
  ]);
  threddsFormControl = new FormControl('', [
    Validators.required,
  ]);
  erddapFormControl = new FormControl('', [
    Validators.required,
  ]);
  addEmptyFormControl = new FormControl('', [
    Validators.required
  ]);

  @ViewChild(DatasetPickerComponent) picker: DatasetPickerComponent;
  header = "The dataset will be added to this list.";
  sub_header = "If you want it further down in the hierarchy, select a data set from the list to navigate to where the new data set should appear.";

  constructor(private addDataService: AdminService,
              private applicationStateService: ApplicationStateService) { }

  ngOnInit() {
  }
  submitNetcdf() {
    const addProperty1:AddProperty = {name: "parent_id", value: this.picker.current_id};
    const addProperty2:AddProperty = {name: "parent_type", value: this.picker.current_type};
    const props = [addProperty1, addProperty2];
    const addnetdf: AddRequest = {
      addProperties: props,
      url: this.griddedSingleFormControl.value,
      type: 'netcdf'
    };
    this.applicationStateService.setForRequest();
    this.addDataService.addDataset(addnetdf).subscribe(data=>{
      var type = data.type
      if ( type == null ) {
        type = 'site'
      }
      this.applicationStateService.setParent(data, type, false);
    },
      error => {
         if ( error.message.includes("auth/login") ) {
           this.applicationStateService.setError("Failed to add netcdf data source. Are you logged in as admin. Reload this page to find out.")
         } else {
           this.applicationStateService.setError("Failed to add netcdf data source.")
         }
         this.error = true;
      }
    )
  }
  submitThredds() {
    const addProperty1:AddProperty = {name: "parent_id", value: this.picker.current_id};
    const addProperty2:AddProperty = {name: "parent_type", value: this.picker.current_type};
    const props = [addProperty1, addProperty2];
    const addthredds: AddRequest = {
      addProperties: props,
      url: this.threddsFormControl.value,
      type: 'thredds'
    };
    this.applicationStateService.setForRequest();
    this.addDataService.addDataset(addthredds).subscribe(data=>{
        // This should be return the parent and re-show the current list.
        this.applicationStateService.setProgress(false);
        this.applicationStateService.setParent(data, this.picker.current_type, false);
      },
      error => {
        if ( error.message.includes("auth/login") ) {
          this.applicationStateService.setError("Failed to add THREDDS data source. Are you logged in as admin. Reload this page to find out.")
        } else {
          this.applicationStateService.setError("Failed to add THREDDS data source.")
        }
        this.error = true;
      }
    )
  }
  submitGriddap() {
    const addProperty1: AddProperty = {name: "parent_id", value: this.picker.current_id}
    const addProperty2: AddProperty = {name: "parent_type", value: this.picker.current_type}
    let props = [addProperty1, addProperty2];
    if (this.use_source_url) {
      const addProperty3:AddProperty = {name: "use_source_url", value: String(this.use_source_url)}
      props.push(addProperty3)
    }
    const addgriddap: AddRequest = {
      addProperties: props,
      url: this.erddapFormControl.value,
      type: 'griddap'
    };
    this.applicationStateService.setForRequest();
    this.addDataService.addDataset(addgriddap).subscribe(data=>{
        console.log("Returned from add data method.");
        this.applicationStateService.setParent(data, this.picker.current_type, false);
      },
      error => {
        if ( error.message.includes("auth/login") ) {
          this.applicationStateService.setError("Failed to add griddap data source. Are you logged in as admin. Reload this page to find out.")
        } else {
          this.applicationStateService.setError("Failed to add griddap data source." + error.message)
        }
        this.error = true;
      }
    )
  }
  submitTabledap() {
    const addProperty1: AddProperty = {name: "parent_id", value: this.picker.current_id}
    const addProperty2: AddProperty = {name: "parent_type", value: this.picker.current_type}
    const props = [addProperty1, addProperty2];
    const addtabledap: AddRequest = {
      addProperties: props,
      url: this.erddapFormControl.value,
      type: 'tabledap'
    };
    this.applicationStateService.setForRequest();
    this.addDataService.addDataset(addtabledap).subscribe(data=>{
        console.log("Returned from add data method.");
        this.applicationStateService.setParent(data, this.picker.current_type, false);
      },
      error => {
        if ( error.message.includes("auth/login") ) {
          this.applicationStateService.setError("Failed to add tabledap data source. Are you logged in as admin. Reload this page to find out.")
        } else {
          this.applicationStateService.setError("Failed to add tabledap data source." + error.message)
        }
        this.error = true;
      }
    )
  }
  submitDsg() {
    const addProperty1:AddProperty = {name: "parent_id", value: this.picker.current_id};
    const addProperty2:AddProperty = {name: "parent_type", value: this.picker.current_type};
    const props = [addProperty1, addProperty2];
    const adddsg: AddRequest = {
      addProperties: props,
      url: this.dsgSingleFormControl.value,
      type: 'dsg'
    };
    this.applicationStateService.setForRequest();
    this.addDataService.addDataset(adddsg).subscribe(data=>{
      this.applicationStateService.setParent(data, this.picker.current_type, false);
    },
      error => {
        if ( error.message.includes("auth/login") ) {
          this.applicationStateService.setError("Failed to add DSG data source. Are you logged in as admin. Reload this page to find out.")
        } else {
          this.applicationStateService.setError("Failed to add DSG data source. " + error.message);
        }
        this.error = true;
      }
    )
  }
  addEmpty() {
    const addProperty1:AddProperty = {name: "parent_id", value: this.picker.current_id};
    const addProperty2:AddProperty = {name: "parent_type", value: this.picker.current_type};
    const addProperty3:AddProperty = {name: "name", value: this.addEmptyFormControl.value};
    const props = [addProperty1, addProperty2, addProperty3];
    const addEmpty: AddRequest = {
      addProperties: props,
      url: null,
      type: 'empty'
    };
    this.applicationStateService.setForRequest();
    this.addDataService.addDataset(addEmpty).subscribe(data=>{
      this.applicationStateService.setParent(data, this.picker.current_type, false);
    },
      error => {
        if ( error.message.includes("auth/login") ) {
          this.applicationStateService.setError("Failed to add container data set. Are you logged in as admin. Reload this page to find out.")
        } else {
          this.applicationStateService.setError("Failed to add container data set.")
        }
        this.error = true;
      }
    );
  }
}
