import {Component, Input, OnInit} from '@angular/core';
import {PropertyBase} from "../property-base";
import {FormGroup} from "@angular/forms";
import {StringProperty} from "../string-property";

@Component({
  selector: 'app-form-property',
  templateUrl: './form-property.component.html',
  styleUrls: ['./form-property.component.css']
})
export class FormPropertyComponent implements OnInit {

  floatLabel = "auto";
  @Input() property: StringProperty;
  @Input() jsonForm: FormGroup;

  constructor() { }

  ngOnInit() {
  }

}
