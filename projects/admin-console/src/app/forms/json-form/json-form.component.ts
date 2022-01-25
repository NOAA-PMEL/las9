import {Component, Input, OnInit} from '@angular/core';
import {PropertyBase} from "../property-base";
import {FormGroup} from "@angular/forms";
import {JsonFormService} from "../../../json-form.service";
import {StringProperty} from "../string-property";

@Component({
  selector: 'app-json-form',
  templateUrl: './json-form.component.html',
  styleUrls: ['./json-form.component.css']
})
export class JsonFormComponent implements OnInit {

  @Input() jsonForm: FormGroup;
  @Input() properties: StringProperty[];
  payload = '';
  constructor() { }

  ngOnInit() {

  }

  onSubmit() {
    console.log("Properties has length " + this.properties.length);
  }
}
