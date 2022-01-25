import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-dataset-delete',
  templateUrl: './dataset-delete.component.html',
  styleUrls: ['./dataset-delete.component.css']
})
export class DatasetDeleteComponent implements OnInit {

  constructor() { }

  header = "Delete a Data Set"
  subHeader = "Select the delete button to permanently remove a data set from LAS. This action cannot be undone."

  ngOnInit() {
  }

}
