import {PropertyBase} from "./property-base";

export class StringProperty extends PropertyBase<string> {
  controlType = 'textbox';
  type: string;

  constructor(options: {} = {}) {
    super(options);
  }
}
