import { Component, OnInit } from '@angular/core';
import {Subscription} from "rxjs";
import {ApplicationStateService} from "../application-state.service";
import {Dataset} from "../json/Dataset";
import {DatasetService} from "../../dataset.service";

@Component({
  selector: 'app-dataset-organize',
  templateUrl: './dataset-organize.component.html',
  styleUrls: ['./dataset-organize.component.css']
})
export class DatasetOrganizeComponent implements OnInit {

  constructor(private applicationStateService: ApplicationStateService,
              private datasetService:DatasetService) { }

  stateChanges: Subscription;
  header:string = "Navigate to the data set you want to move.";
  sub_header = "Click on the move button of data set(s) you want to move.";
  ngOnInit() {
  }

}
