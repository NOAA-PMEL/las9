import {StringProperty} from "./app/forms/string-property";
import {FormControl, FormGroup} from "@angular/forms";

export class JsonFormService {
  form: FormGroup;
  constructor() { }
  makeFormGroup(properties: any[]) {
    let group: any = {};
    properties.forEach(property => {
         if ( property instanceof StringProperty ) {
           group[property.key] = new FormControl(property.value);
         }
      }
    )
    return new FormGroup(group);
  }
}
