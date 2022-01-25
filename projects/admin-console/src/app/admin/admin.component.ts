import {Component, OnInit} from '@angular/core';
import {AdminService} from "../../admin.service";
import {DatasetService} from "../../dataset.service";
import {ApplicationStateService} from "../application-state.service";

/** Error when invalid control is dirty, touched, or submitted. */

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.css']
})
export class AdminComponent implements OnInit {
  constructor(public datasetService: DatasetService,
              public addDataService: AdminService,
              public applicationStateService: ApplicationStateService,
              ) { }

  ngOnInit() {
    this.applicationStateService.setForRequest();
    this.datasetService.getSite().subscribe(site => {
      this.applicationStateService.setParent(site, 'site', true);
    },
      error => {
        this.applicationStateService.setError("Failed to get site information from the server.")
      }
    );
  }
  loadSecondary(event) {
    const index = event.index;
    if ( index === 3 ) {
      this.applicationStateService.setForRequest();
      this.datasetService.getSite().subscribe(site => {
        this.applicationStateService.setSecondary(site, 'site', true);
        this.applicationStateService.setProgress(false);
      },
      error => {
        this.applicationStateService.setError("Failed to get the site information from the server.")
      }
      );
    } else if ( index === 4 ) {
      this.applicationStateService.setForRequest();
      this.datasetService.getPrivate().subscribe(site => {
        this.applicationStateService.setSecondary(site, 'site', true);
        this.applicationStateService.setProgress(false);
      },
      error => {
        this.applicationStateService.setError("Failed to get the site information from the server.")
      }
      );
    }
  }
}
