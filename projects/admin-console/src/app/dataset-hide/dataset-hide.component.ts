import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-dataset-hide',
  templateUrl: './dataset-hide.component.html',
  styleUrls: ['./dataset-hide.component.css']
})
export class DatasetHideComponent implements OnInit {

  constructor() { }
  header = "Hide data from the public web site."
  subHeader="Any data set you move this private area will be invisible to users until you move it back."
  right_header="Data sets listed here are hidden from users.";
  right_sub_header="Click \"show\" to move it back to the location shown on the left.";
  ngOnInit() {
  }

}
