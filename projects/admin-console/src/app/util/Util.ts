export class Util {
  // Returns if a value is really a number
  static isNumber (value) {
    return typeof value === 'number' && isFinite(value);
  }
  // Returns if a value is a string
  static isString (value) {
    return typeof value === 'string' || value instanceof String;
  }
  // Returns if a value is an Object
  static isObject (value) {
    return value && typeof value === 'object' && value.constructor === Object;
  }
  static isArray(value) {
    return Array.isArray(value);
  }
}
