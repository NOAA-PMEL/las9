import {Component, Input, OnInit} from '@angular/core';
import {Dataset} from "../json/Dataset";
import {DatasetService} from "../../dataset.service";
import {Site} from "../json/Site";
import {Subscription} from "rxjs";
import {ApplicationStateService} from "../application-state.service";
import {AddProperty} from "../../add-property";
import {AddRequest} from "../../add-request";
import {AdminService} from "../../admin.service";

@Component({
  selector: 'app-dataset-picker',
  templateUrl: './dataset-picker.component.html',
  styleUrls: ['./dataset-picker.component.css'],
})

export class DatasetPickerComponent implements OnInit {

  breadcrumbs;
  datasets;
  errorDialogMessage: string;
  error;

  current_type;
  current_id;

  @Input()
  header;
  @Input()
  subHeader;
  @Input()
  edit: boolean = false;
  @Input()
  move: boolean = false;
  @Input()
  hide: boolean = false;
  @Input()
  delete: boolean = false;
  @Input()
  update: boolean = false;

  @Input()
  side_by_side: boolean = false;

  secondary_breadcrumbs = [];
  secondary_datasets;
  current_secondary_type;



  stateChanges: Subscription;
  constructor(private datasetService: DatasetService,
              private adminService: AdminService,
              private applicationStateService: ApplicationStateService) { }

  ngOnInit() {
    this.breadcrumbs = [];
    this.secondary_breadcrumbs = [];

    this.stateChanges = this.applicationStateService.stateChanged.subscribe(state => {
      if (state) {
        if ( state.parent && !state.showProgress) {
          this.current_type = state['parent_type'];
          this.current_id = state.parent.id;
          if (state.add_breadcrumb) this.addBreadcrumb(state.parent);
          this.doPick(state['parent']);
          if (this.side_by_side) {
            // Won't be there until the tab is revealed
            if (state.secondary) {
              this.current_secondary_type = state.secondary_type;
              if (state.add_secondary_breadcrumb) this.addSecondaryBreadcrumb(state.secondary);
              this.doSecondary(state['secondary']);
            }
          }
        }
      }
    });
  }
  getDataset(dataset: Dataset) {
    if ( dataset.variableChildren ) {
      this.errorDialogMessage = "This data set contains variables, there are no more data sets below this point."
      this.error = true;
    } else {
      this.applicationStateService.setForRequest();
      this.datasetService.getDataset(dataset.id).subscribe(indataset => {
        this.applicationStateService.setParent(indataset, 'dataset', true);
      },
        error => {
          this.error = true;
          this.applicationStateService.setProgress(false);
          if (error.message.includes("auth/login")) {
            this.errorDialogMessage = "Unable to access the admin server. Are you logged in as admin? Reload this page to login."
          } else {
            this.errorDialogMessage = "Failed to get data set information from server.";
          }
        }
      );
    }
  }
  getSecondaryDataset(dataset: Dataset) {
    if ( dataset.variableChildren ) {
      this.errorDialogMessage = "This data set contains variables, there are no more data sets below this point."
      this.error = true;
    }
    this.applicationStateService.setForRequest();
    this.datasetService.getDataset(dataset.id).subscribe(indataset => {
      this.applicationStateService.setSecondary(indataset, 'dataset', true);
    },
      error => {
        this.error = true;
        this.applicationStateService.setProgress(false);
        if (error.message.includes("auth/login")) {
          this.errorDialogMessage = "Unable to access the admin server. Are you logged in as admin? Reload this page to login."
        } else {
          this.errorDialogMessage = "Failed to get data set information from server. " + error.message;
        }
      }
    );
  }
  editDataset(dataset: Dataset) {
    this.applicationStateService.setForRequest();
    this.datasetService.getDataset(dataset.id).subscribe(indataset => {
      this.applicationStateService.setDatasetToEdit(indataset);
    },
      error => {
         this.error = true;
        this.applicationStateService.setProgress(false);
        if (error.message.includes("auth/login")) {
          this.errorDialogMessage = "Unable to access the admin server. Are you logged in as admin? Reload this page to login."
        } else {
          this.errorDialogMessage = "Failed to get data set details to edit. " + error.message;
        }
      }
    );
  }
  editUpdate(dataset: Dataset) {
    this.datasetService.getDataset(dataset.id).subscribe(indataset => {
        this.applicationStateService.setDatasetToEditUpdates(indataset);
      },
      error => {
        this.error = true;
        this.applicationStateService.setProgress(false);
        if (error.message.includes("auth/login")) {
          this.errorDialogMessage = "Unable to access the admin server. Are you logged in as admin? Reload this page to login."
        } else {
          this.errorDialogMessage = "Failed to get data set details to edit. " + error.message;
        }
      }
    );
  }
  deleteDataset(dataset: Dataset) {
    this.applicationStateService.setForRequest();
    this.adminService.deleteDataset(dataset.id).subscribe(indataset => {
      this.applicationStateService.setParent(indataset, this.current_type, false);
    },
      error => {
         this.error = true;
        this.applicationStateService.setProgress(false);
        if (error.message.includes("auth/login")) {
          this.errorDialogMessage = "Unable to access the admin server. Are you logged in as admin? Reload this page to login."
        } else {
          this.errorDialogMessage = "Unexpected error deleting data set. " + error.message;
        }
      }
    );
  }

  moveDataset(from_dataset: Dataset) {
    const dataset = this.applicationStateService.getSecondary();
    const parent = this.applicationStateService.getParent()
    if ( parent.id === dataset.id && this.current_type === this.current_secondary_type ) {
      this.errorDialogMessage = "The source and destination appear to be the same. Nothing to move."
      this.error = true;
    } else {
      const addProperty1: AddProperty = {name: "move_from_id", value: from_dataset.id.toString()};
      const addProperty2: AddProperty = {name: "move_to_id", value: dataset.id.toString()};
      const addProperty3: AddProperty = {name: "move_to_type", value: this.current_secondary_type};
      const props = [addProperty1, addProperty2, addProperty3];
      const moveDataset: AddRequest = {
        addProperties: props,
        url: '',
        type: 'move'
      };
      this.applicationStateService.setForRequest();
      this.adminService.moveDataset(moveDataset).subscribe(data => {
        const parent = data.origin;
        const dest = data.destination;
          this.applicationStateService.setParentAndSecondary(parent, this.current_type, dest, this.current_secondary_type, false, false)
        },
          error => {
             this.error = true;
            this.applicationStateService.setProgress(false);
            if (error.message.includes("auth/login")) {
              this.errorDialogMessage = "Unable to access the admin server. Are you logged in as admin? Reload this page to login."
            } else {
              this.errorDialogMessage = "Unexpected error trying to move the data set. " + error.message;
            }
          }
      )
    }
  }
  showDataset(from_dataset: Dataset) {
    const dataset =  this.applicationStateService.getParent();
    const addProperty1: AddProperty = {name: "move_from_id", value: from_dataset.id.toString()};
    const addProperty2: AddProperty = {name: "move_to_id", value: dataset.id.toString()};
    const addProperty3: AddProperty = {name: "move_to_type", value: this.current_type};
    const props = [addProperty1, addProperty2, addProperty3];
    const moveDataset: AddRequest = {
      addProperties: props,
      url: '',
      type: 'show'
    };
    this.applicationStateService.setForRequest();
    this.adminService.moveDataset(moveDataset).subscribe(data => {
        const parent = data.origin;
        const dest = data.destination;
        this.applicationStateService.setParentAndSecondary(parent, this.current_type, dest, this.current_secondary_type, false, false)
      },
        error => {
          this.error = true;
          this.applicationStateService.setProgress(false);
          if (error.message.includes("auth/login")) {
            this.errorDialogMessage = "Unable to access the admin server. Are you logged in as admin? Reload this page to login."
          } else {
            this.errorDialogMessage = "Unexpected error trying to retieve data set. " + error.message;
          }
        }
    )
  }
  doPick(indataset: any) {
    if (indataset != null ) {
      if ( indataset.status === "Ingest failed") {
        this.errorDialogMessage = indataset.message;
        this.error = true;
      } else {
        if ( indataset.datasets != null ) {
          this.datasets = indataset.datasets;
        }
      }
    }
  }
  doSecondary(dataset: any) {
    if ( dataset.datasets != null ) {
      this.secondary_datasets = dataset.datasets;
    }
  }
  addSecondaryBreadcrumb(container: any) {
    let pid = 1;
    let bc_title = 'Site'
    this.secondary_breadcrumbs = []
    if ( container.parent ) {
      pid = container.parent.id;
      bc_title = container.parent.title;
    }
    this.secondary_breadcrumbs.push({label: "Up", command: (event)=> {
        this.doSecondaryBreadcrumb(bc_title, pid)
      }})
    this.secondary_breadcrumbs.push({label: container.title, command: (event)=> {
        this.doSecondaryBreadcrumb(container.title, container.id)
      }})
  }
  addBreadcrumb(parent: any) {
    // Breadcrubs are going to be the parent and the item (known here as parent)
    let pid = 1;
    let bc_title = 'Site'
    this.breadcrumbs = []
    if ( parent.parent ) {
      pid = parent.parent.id;
      bc_title = parent.parent.title;
    }
    this.breadcrumbs.push({label: "Up", command: (event)=> {
        this.doBreadcrumb(bc_title, pid)
      }})
    this.breadcrumbs.push({label: parent.title, command: (event)=> {
        this.doBreadcrumb(parent.title, parent.id)
      }})
  }
  doSecondaryBreadcrumb(title: string, id: number) {
    this.applicationStateService.setForRequest();
    if ( title === 'Site' ) {
      this.datasetService.getSite().subscribe(site =>{
        this.applicationStateService.setSecondary(site, 'site', true)
      });
    } else {
      this.datasetService.getDataset(id).subscribe(indataset => {
        this.applicationStateService.setSecondary(indataset, 'dataset', true);
      });
    }
  }
  doBreadcrumb(title: string, id: number) {
    this.applicationStateService.setForRequest();
    if (title === 'Site') {
      this.datasetService.getSite().subscribe(site => {
        this.applicationStateService.setParent(site, 'site', true)
      });
    } else {
      this.datasetService.getDataset(id).subscribe(indataset => {
        this.applicationStateService.setParent(indataset, 'dataset', true);
      });
    }
  }
  doSite(site: Site) {
    this.breadcrumbs = [];
    this.current_id = site.id;
    this.current_type = "site";
    this.breadcrumbs.push({label: site.title, command: ()=>{this.doBreadcrumb(site.title, site.id)}})
    this.datasets = site.datasets;
  }
}
