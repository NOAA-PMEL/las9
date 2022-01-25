import {PropertyBase} from "./property-base";

export class StringArrayProperty extends PropertyBase<string[]> {
  controlType = "dropdown";
  options: {value: string}[] = [];
  constructor(options: {} = {}) {
    super(options);
  }
}
