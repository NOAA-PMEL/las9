import {Dataset} from "./json/Dataset";

export interface ApplicationStateStore {

  showProgress: boolean;
  errorDialogMessage: string;
  requestError: boolean;

  // Objects related to the primary data picker
  parent: any;
  parent_type: string;
  add_breadcrumb: boolean;


  // Second independent popup picker for the destination for moving a data set
  secondary: any;
  secondary_type: string;
  add_secondary_breadcrumb: boolean;
  destination_parent: any;
  destination: any;
  dataset_to_move: Dataset;
  dataset_to_edit_updates: Dataset;

  // Stuff related to the data set selected for editing.
  edit_dataset: Dataset;
  dataset_dirty: {[key: string]: {}};
  variables_dirty: Dirty;
  geox_dirty: Dirty;
  geoy_dirty: Dirty;
  z_dirty: Dirty;
  time_dirty: Dirty;
}
export interface Dirty {
  [id: number]: {}
}
