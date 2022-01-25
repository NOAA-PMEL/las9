import {Component, OnInit} from '@angular/core';
import {Subscription} from "rxjs";
import {ApplicationStateService} from "../application-state.service";
import {Dataset, Variable, Vector} from "../json/Dataset";
import {StringProperty} from "../forms/string-property";
import {JsonFormService} from "../../json-form.service";
import {FormGroup} from "@angular/forms";
import {Util} from "../util/Util";
import {AdminService} from "../../admin.service";

@Component({
  selector: 'app-dataset-edit',
  templateUrl: './dataset-edit.component.html',
  styleUrls: ['./dataset-edit.component.css']
})
export class DatasetEditComponent implements OnInit {
  stateChanges: Subscription;
  edit_dataset: Dataset;
  editing_dataset;
  jsonForm: FormGroup;
  title;
  variables: Variable[];
  vectors: Vector[];
  edit_variable;
  editing_variable;
  variables_title;
  variable_title;
  variable_properties: StringProperty[] = [];
  axes_title;
  edit_geoAxisX;
  editing_geoAxisX;
  geoAxisX_properties;
  geoAxisXForm: FormGroup;
  hasX;
  edit_geoAxisY;
  geoAxisY_properties;
  editing_geoAxisY;
  geoAxisYForm: FormGroup;
  hasY;
  edit_verticalAxis;
  editing_verticalAxis;
  verticalForm: FormGroup;
  vertical_axis_properties;
  hasZ
  edit_timeAxis;
  editing_timeAxis;
  hasT;
  time_axis_properties
  timeForm;
  hasV;

  header = "Navigate to the data set you want to edit.";
  sub_header = "Click the edit icon next to the data set name."
  edit: boolean = true;
  properties: StringProperty[] = [];

  variableForm: FormGroup;
  constructor(private applicationStateService: ApplicationStateService,
              private formService: JsonFormService,
              private adminService: AdminService) { }
  ngOnInit() {
    this.stateChanges = this.applicationStateService.stateChanged.subscribe(state => {
      if (state) {
        // If something else is open, refresh that. Otherwise refresh the data set.
        if ( this.editing_variable ) {
          console.log("State change with editing_variable.")
        } else if ( this.editing_geoAxisX ) {
          console.log("State change with editing_geoAxisX")
        } else if ( this.editing_geoAxisY ) {
          console.log("State change with editing_geoAxisY")
        } else if ( this.editing_verticalAxis ) {
          console.log("State change with editing_vertialAxis")
        } else if ( this.editing_timeAxis ) {
          console.log("State change with editing_timeAxis")
        } else {
          console.log("State change with what should be the data set page open.")
          this.edit_dataset = state.edit_dataset;
          if (this.edit_dataset) {
            this.hasV = this.edit_dataset.variableChildren;
            this.variables = [];
            this.vectors = [];
            this.properties = [];
            let has_url = false;
            for (let prop in this.edit_dataset) {
              if (this.edit_dataset[prop]) {
                if (this.edit_dataset[prop] instanceof String || typeof this.edit_dataset[prop] === 'string') {
                  if ( prop == 'url') has_url = true;
                  let sp: StringProperty = new StringProperty({label: prop, value: this.edit_dataset[prop], key: prop})
                  this.properties.push(sp);
                } else if (Util.isArray(this.edit_dataset[prop])) {
                  if (prop === "variables") {
                    this.variables = this.edit_dataset[prop];
                  } else if ( prop === "vectors" ) {
                    this.vectors = this.edit_dataset[prop];
                  }
                } else {
                  // Other types of things to show in form
                }
              }
            }
            if ( !has_url ) {
              let sp: StringProperty = new StringProperty({label: 'url', value: '', key: 'url'})
              this.properties.push(sp);
            }
            this.jsonForm = this.formService.makeFormGroup(this.properties);
            this.title = "Editing " + this.edit_dataset.title;
            this.variables_title = "Variables for " + this.edit_dataset.title + "   (Deleting a variable takes place immediately! Edits are local until you push save.)";
            this.editing_dataset = true;
          }
        }
      }
    });
  }
  close() {
    this.applicationStateService.reinit();
  }
  addVectors() {
    let id = this.edit_dataset.id;
    this.applicationStateService.setForRequest();
    this.adminService.addVectors(id).subscribe(data => {
      this.applicationStateService.setProgress(false);
      this.applicationStateService.setDatasetToEdit(data);
      // In this case wait for the return to happen before closing
      this.applicationStateService.reinit();
    },error => {
        if ( error.message.includes("auth/login") ) {
          this.applicationStateService.setError("Failed to add vectors. Are you logged in as admin. Reload the page to login.");
        } else {
          this.applicationStateService.setError("Failed to add vectors. Is the server running.")
        }
      }
    );
  }
  save() {

    const dirty = this.getDirtyValues(this.jsonForm);
    if ( Object.keys(dirty).length > 0 ) {
      dirty["id"] = this.edit_dataset.id;
      if (Object.keys(dirty).length > 0) {
        for (let key in dirty) {
          this.edit_dataset[key] = dirty[key];
        }
      }
      this.applicationStateService.setDatasetDirty(this.edit_dataset, dirty);
    }

    const parent_type = this.applicationStateService.getParentType('parent_type');
    if ( this.applicationStateService.isDirty() ) {
      this.applicationStateService.setForRequest();
      const changes = this.applicationStateService.getDirty();
      this.adminService.saveDataset(changes).subscribe(data => {
        this.applicationStateService.setParent(data, parent_type, false);
        // In this case wait for the return to happen before closing
        this.applicationStateService.reinit();
        this.editing_dataset = false;
      },
        error => {
           if ( error.message.includes("auth/login") ) {
             this.applicationStateService.setError("Failed to save changes. Are you logged in as admin. Reload the page to login.");
           } else {
             this.applicationStateService.setError("Failed to save changes. Is the server running.")
           }
        }
        );
    } else {
      this.applicationStateService.reinit();
      this.editing_dataset = false;
    }
  }
  deleteVariable(invariable: Variable) {
    this.applicationStateService.setForRequest();
    this.adminService.deleteVariable(invariable.id).subscribe(indataset => {
        this.applicationStateService.setProgress(false);
        this.applicationStateService.setDatasetToEdit(indataset);
      },
      error => {
        this.applicationStateService.setProgress(false);
        if (error.message.includes("auth/login")) {
          this.applicationStateService.setError("Unable to access the admin server. Are you logged in as admin? Reload this page to login.");
        } else {
          this.applicationStateService.setError("Unexpected error deleting variable. " + error.message);
        }
      }
    );
  }
  deleteVector(invector: Vector) {
    this.applicationStateService.setForRequest();
    this.adminService.deleteVector(invector.id).subscribe(indataset => {
        this.applicationStateService.setProgress(false);
        this.applicationStateService.setDatasetToEdit(indataset);
      },
      error => {
        this.applicationStateService.setProgress(false);
        if (error.message.includes("auth/login")) {
          this.applicationStateService.setError("Unable to access the admin server. Are you logged in as admin? Reload this page to login.");
        } else {
          this.applicationStateService.setError("Unexpected error deleting variable. " + error.message);
        }
      }
    );
  }
  editVariable(variable: Variable) {
    this.editing_dataset = false;
    this.edit_variable = variable;
    this.variable_properties = [];
    for (let prop in this.edit_variable) {
      if (this.edit_variable[prop]) {
        if (Util.isString(this.edit_variable[prop]) ) {
          let sp: StringProperty = new StringProperty({label: prop, value: this.edit_variable[prop], key: prop})
          this.variable_properties.push(sp);
        } else if ( Util.isObject(this.edit_variable[prop]) ) {
          if (prop === "geoAxisX") {
            this.hasX = true;
            this.edit_geoAxisX = this.edit_variable[prop]
            this.axes_title = "Axes for " + this.edit_variable.title;
          } else if (prop === "geoAxisY") {
            this.hasY = true;
            this.edit_geoAxisY = this.edit_variable[prop]
            this.axes_title = "Axes for " + this.edit_variable.title;
          } else if (prop === "verticalAxis") {
            this.hasZ = true;
            this.edit_verticalAxis = this.edit_variable[prop]
            this.axes_title = "Axes for " + this.edit_variable.title;
          } else if (prop === "timeAxis") {
            this.hasT = true;
            this.edit_timeAxis = this.edit_variable[prop]
            this.axes_title = "Axes for " + this.edit_variable.title;
          }
        }
      }
    }
    this.variableForm = this.formService.makeFormGroup(this.variable_properties);
    this.variable_title = "Editing " + variable.title;
    this.editing_variable = true;
  }
  done_variable() {
    const dirty = this.getDirtyValues(this.variableForm);
    if ( Object.keys(dirty).length > 0 ) {
      for (let key in dirty) {
        this.edit_variable[key] = dirty[key];
      }
      this.applicationStateService.addVariableDirty(this.edit_dataset, this.edit_variable.id, dirty);
    }
    this.editing_variable = false;
    this.editing_dataset = true;
  }
  editX() {
    this.done_variable();
    this.geoAxisX_properties = [];
    for (let prop in this.edit_geoAxisX) {
      if (this.edit_geoAxisX[prop]) {
        if (Util.isString(this.edit_geoAxisX[prop])) {
          let sp: StringProperty = new StringProperty({label: prop, value: this.edit_geoAxisX[prop], key: prop});
          this.geoAxisX_properties.push(sp);
        } else if ( Util.isNumber(this.edit_geoAxisX[prop]) || isNaN(this.edit_geoAxisX[prop])) {
          // TODO introduce a NumberProperty and then use string and number validators on the respective fields
          let np: StringProperty = new StringProperty({label: prop, value: this.edit_geoAxisX[prop], key: prop});
          this.geoAxisX_properties.push(np);
        }
      }
    }
    this.geoAxisXForm = this.formService.makeFormGroup(this.geoAxisX_properties);
    this.editing_variable = false;
    this.editing_geoAxisX = true;
  }
  doneX() {
    const dirty = this.getDirtyValues(this.geoAxisXForm);
    this.editing_variable = true;
    this.editing_geoAxisX = false;
    if ( Object.keys(dirty).length > 0 ) {
      for (let key in dirty) {
        this.edit_geoAxisX[key] = dirty[key];
      }
      this.applicationStateService.addGeoAxisXDirty(this.edit_dataset, this.edit_geoAxisX.id, dirty);
    }
  }
  // The y axis
  editY() {
    this.done_variable();
    this.geoAxisY_properties = [];
    for (let prop in this.edit_geoAxisY) {
      if (this.edit_geoAxisY[prop]) {
        if (Util.isString(this.edit_geoAxisY[prop])) {
          let sp: StringProperty = new StringProperty({label: prop, value: this.edit_geoAxisY[prop], key: prop});
          this.geoAxisY_properties.push(sp);
        } else if ( Util.isNumber(this.edit_geoAxisY[prop]) || isNaN(this.edit_geoAxisY[prop])) {
          // TODO introduce a NumberProperty and then use string and number validators on the respective fields
          let np: StringProperty = new StringProperty({label: prop, value: this.edit_geoAxisY[prop], key: prop});
          this.geoAxisY_properties.push(np);
        }
      }
    }
    this.geoAxisYForm = this.formService.makeFormGroup(this.geoAxisY_properties);
    this.editing_variable = false;
    this.editing_geoAxisY = true;
  }
  doneY() {
    const dirty = this.getDirtyValues(this.geoAxisYForm);
    for ( let key in dirty ) {
      this.edit_geoAxisY[key] = dirty[key];
    }
    this.applicationStateService.addGeoAxisYDirty(this.edit_dataset, this.edit_geoAxisY.id, dirty);
    this.editing_variable = true;
    this.editing_geoAxisY = false;
  }
  // The z axis
  editZ() {
    this.done_variable();
    this.vertical_axis_properties = [];
    for (let prop in this.edit_verticalAxis) {
      if (this.edit_verticalAxis[prop]) {
        if (Util.isString(this.edit_verticalAxis[prop])) {
          let sp: StringProperty = new StringProperty({label: prop, value: this.edit_verticalAxis[prop], key: prop});
          this.vertical_axis_properties.push(sp);
        } else if ( Util.isNumber(this.edit_verticalAxis[prop]) || isNaN(this.edit_verticalAxis[prop])) {
          // TODO introduce a NumberProperty and then use string and number validators on the respective fields
          let np: StringProperty = new StringProperty({label: prop, value: this.edit_verticalAxis[prop], key: prop});
          this.vertical_axis_properties.push(np);
        }
      }
    }
    this.verticalForm = this.formService.makeFormGroup(this.vertical_axis_properties);
    this.editing_variable = false;
    this.editing_verticalAxis = true;
  }
  doneZ() {
    const dirty = this.getDirtyValues(this.verticalForm);
    for ( let key in dirty ) {
      this.edit_verticalAxis[key] = dirty[key];
    }
    this.applicationStateService.addVerticalDirty(this.edit_dataset, this.edit_verticalAxis.id, dirty);
    this.editing_variable = true;
    this.editing_verticalAxis = false;
  }
  editT() {
    this.done_variable();
    this.time_axis_properties = [];
    for (let prop in this.edit_timeAxis) {
      if (this.edit_timeAxis[prop]) {
        if (Util.isString(this.edit_timeAxis[prop])) {
          let sp: StringProperty = new StringProperty({label: prop, value: this.edit_timeAxis[prop], key: prop});
          this.time_axis_properties.push(sp);
        } else if ( Util.isNumber(this.edit_timeAxis[prop]) || isNaN(this.edit_timeAxis[prop])) {
          // TODO introduce a NumberProperty and then use string and number validators on the respective fields
          let np: StringProperty = new StringProperty({label: prop, value: this.edit_timeAxis[prop], key: prop});
          this.time_axis_properties.push(np);
        }
      }
    }
    this.timeForm = this.formService.makeFormGroup(this.time_axis_properties);
    this.editing_variable = false;
    this.editing_timeAxis = true;
  }
  doneT() {
    const dirty = this.getDirtyValues(this.timeForm);
    for ( let key in dirty ) {
      this.edit_timeAxis[key] = dirty[key];
    }
    this.applicationStateService.addTimeDirty(this.edit_dataset, this.edit_timeAxis.id, dirty);
    this.editing_variable = true;
    this.editing_timeAxis = false;
  }

  getDirtyValues(cg: FormGroup) {
    const dirtyValues = {};
    Object.keys(cg.controls).forEach(c => {
      const currentControl = cg.get(c);

      if (currentControl.dirty) {
        dirtyValues[c] = currentControl.value;
      }
    });
    return dirtyValues;
  }
}
