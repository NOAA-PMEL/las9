import { Component, OnInit } from '@angular/core';
import {AdminService} from "../../admin.service";
import {ApplicationStateService} from "../application-state.service";
import {Backup} from "../json/Dataset";
import {DatasetService} from "../../dataset.service";

@Component({
  selector: 'app-backup-restore',
  templateUrl: './backup-restore.component.html',
  styleUrls: ['./backup-restore.component.css']
})
export class BackupRestoreComponent implements OnInit {

  header: string = "Restore"
  subHeader: string = "Restore from an existing backup, Delete unneeded backups."

  bheader: string = "Backup"
  bsubHeader: string="Create a new backup."
  backups: Backup[]
  constructor(public datasetService: DatasetService,
              public applicationStateService: ApplicationStateService,
              public adminService: AdminService) { }

  ngOnInit(): void {
    this.applicationStateService.setForRequest()
    this.adminService.listBackups().subscribe(data => {
      this.backups = data;
    });
  }
  restore(backup: Backup) {
    this.applicationStateService.setForRequest();
    this.adminService.restore(backup).subscribe(data => {
      this.applicationStateService.setProgress(false);
      this.backups = data;
      this.getSite();
    });
  }
  backup() {
    this.applicationStateService.setForRequest();
    this.adminService.backup().subscribe(data => {
      this.applicationStateService.setProgress(false);
      this.backups = data;
    });
  }
  delete(backup: Backup) {
    this.applicationStateService.setForRequest();
    this.adminService.deleteBackup(backup).subscribe(data => {
      this.applicationStateService.setProgress(false);
      this.backups = data;
    });
  }
  getSite() {
    // The site was restored from backup. Get it and reset the UI.
    this.datasetService.getSite().subscribe(site => {
        this.applicationStateService.setProgress(false);
        this.applicationStateService.setParent(site, 'site', true);
      },
      error => {
        this.applicationStateService.setError("Failed to get site information from the server.")
      }
    );
  }
}
