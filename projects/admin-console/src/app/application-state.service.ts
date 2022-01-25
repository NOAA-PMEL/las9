import {ObservableStore} from "@codewithdan/observable-store";
import {ApplicationStateStore} from "./application-state-store";
import {Dataset, DatasetProperty} from "./json/Dataset";
import {Util} from "./util/Util";

export class ApplicationStateService extends ObservableStore<ApplicationStateStore>{

  error: boolean = false;

  constructor() {
    super({ trackStateHistory: true, logStateChanges: true });
  }
  setParentAndSecondary(parent: any, parent_type: string, secondary: any, secondary_type: string, breadcrumb: boolean, secondary_breadcrumb: boolean) {
    this.setState({showProgress: false, parent: parent, parent_type: parent_type, add_breadcrumb: breadcrumb, secondary: secondary, secondary_type: secondary_type, add_secondary_breadcrumb: secondary_breadcrumb, edit_dataset: null}, 'SET_BOTH');
  }
  setParent(parent: any, type: string, breadcrumb: boolean) {
    this.error = false;
    this.setState({showProgress: false, errorDialogMessage: "", parent: parent, parent_type: type, add_breadcrumb: breadcrumb, add_secondary_breadcrumb: false, edit_dataset: null}, 'SET_PARENT');
  }
  setProgress(show: boolean) {
    this.setState({showProgress: show})
  }
  getProgress(): boolean {
    let state = this.getState();
    if ( state ) {
      return state.showProgress;
    }
    return false;
  }
  // Set for request, start the progress meter and clear the previous error.
  setForRequest() {
    this.error = false;
    this.setState({showProgress: true, errorDialogMessage: "", add_breadcrumb: false})
  }
  setSecondary(container: any, type: string, breadcrumb: boolean) {
    this.setState({showProgress: false, secondary: container, secondary_type: type, add_secondary_breadcrumb: breadcrumb, add_breadcrumb: false, edit_dataset: null}, 'SET_SECONDARY');
  }
  getSecondary():any{
    let state = this.getState();
    if ( state ) {
      return state.secondary;
    }
  }
  setDatasetToMove(dataset: Dataset) {
    this.setState({showProgress: false, dataset_to_move: dataset, add_breadcrumb: false}, 'MOVE_DATASET');
  }
  setDatasetToEdit(dataset: Dataset) {
    this.setState({showProgress: false, edit_dataset: dataset}, 'EDIT_DATASET');
  }
  setDatasetToEditUpdates(dataset: Dataset) {
    this.setState({showProgress: false, dataset_to_edit_updates: dataset}, 'EDIT_UPDATE_DATASET');
  }
  setDatasetDirty(edit_dataset: Dataset, dataset_dirty: any) {
    let state = this.getState();
    dataset_dirty.id = state.edit_dataset.id;
    this.setState({edit_dataset: edit_dataset, dataset_dirty: dataset_dirty}, 'DATASET_CHANGE');
  }
  addVariableDirty(edit_dataset: Dataset, id: number, variable_dirty: any) {
    let state = this.getState();
    let dirty_variables = state.variables_dirty;
    if ( !Util.isObject(dirty_variables) ) {
      dirty_variables = {};
    }
    dirty_variables[id] = variable_dirty;
    this.setState({edit_dataset: edit_dataset, variables_dirty: dirty_variables}, "VARIABLE_CHANGE");
  }
  addGeoAxisXDirty(edit_dataset: Dataset, id: number, geox_dirty: any) {
    let state = this.getState();
    let x_dirty = state.geox_dirty;
    if ( !Util.isObject(x_dirty)) {
      x_dirty = {};
    }
    x_dirty[id] = geox_dirty;
    this.setState({edit_dataset: edit_dataset, geox_dirty: x_dirty}, "X_AXIS_CHANGE")
  }
  addGeoAxisYDirty(edit_dataset: Dataset, id: number, geoy_dirty: any) {
    let state = this.getState();
    let y_dirty = state.geoy_dirty;
    if ( !Util.isObject(y_dirty) ) {
      y_dirty = {};
    }
    y_dirty[id] =  geoy_dirty;
    this.setState({edit_dataset: edit_dataset, geoy_dirty: y_dirty}, "Y_AXIS_CHANGE")
  }
  addVerticalDirty(edit_dataset: Dataset, id: number, vertical_dirty: any) {
    let state = this.getState();
    let vert_dirty = state.z_dirty;
    if ( !Util.isObject(vert_dirty) ) {
      vert_dirty = {};
    }
    vert_dirty[id] = vertical_dirty
    this.setState({edit_dataset: edit_dataset, z_dirty: vert_dirty}, "Z_AXIS_CHANGE");
  }
  addTimeDirty(edit_dataset: Dataset, id: number, time_dirty: any) {
    let state = this.getState();
    let time_d = state.time_dirty;
    if ( !Util.isObject(time_d) ) {
      time_d = {};
    }
    time_d[id] = time_dirty;
    this.setState({edit_dataset: edit_dataset, time_dirty: time_d}, "TIME_AXIS_CHANGE")
  }
  isDirty() {
    let state = this.getState();
    return state.time_dirty || state.z_dirty || state.geoy_dirty || state.geox_dirty || state.dataset_dirty || state.variables_dirty;
  }
  getDirty(): Changes {
    let state = this.getState();
    // Only the non-null entries get serialized. :-)
    let changes: Changes =  {
      dataset: state.dataset_dirty,
      geoAxisX: state.geox_dirty,
      geoAxisY: state.geoy_dirty,
      timeAxis: state.time_dirty,
      variables: state.variables_dirty,
      verticalAxis: state.z_dirty
    };
    return changes;
  }
  setError(message: string) {
    this.error = true;
    this.setState({showProgress: false, errorDialogMessage: message});
  }
  getErrorMessage(): string {
    let state = this.getState();
    if ( state ) {
      return state.errorDialogMessage;
    }
    return "";
  }

  reinit() {
    this.error = false;
    this.setState({dataset_to_edit_updates: null, showProgress: false, errorDialogMessage: "", edit_dataset: null, dataset_dirty: null, variables_dirty: null, geox_dirty: null, geoy_dirty: null, z_dirty: null, time_dirty: null}, "REINIT_STATE")
  }
  getEditDataset(): Dataset {
    let state = this.getState();
    if ( state ) {
      return state.edit_dataset;
    }
  }
  getMoveDataset(): Dataset {
    let state = this.getState();
    if ( state ) {
      return state.dataset_to_move;
    }
  }
  getParentType(state_token_type: string): string {
    let state = this.getState();
    if ( state ) {
      return state[state_token_type];
    }
  }
  getParent() {
    let state = this.getState();
    if ( state) {
      return state.parent;
    }
  }
}
export interface Changes {
  dataset: any,
  variables: any,
  geoAxisX: any,
  geoAxisY: any,
  verticalAxis: any,
  timeAxis: any
}
export interface UpdateSpec {
  datset: any,
  cron_spec: string
}

