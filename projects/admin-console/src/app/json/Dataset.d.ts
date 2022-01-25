export interface Backup {
  directory: string;
  highlight: boolean;
}
export interface Dataset {
  datasetProperties?: (DatasetProperty)[] | null;
  datasets?: (Dataset)[] | null;
  geometry: string;
  hash: string;
  history: string;
  message: string;
  id: number;
  parent?: Dataset;
  status: string;
  title: string;
  type: string;
  url: string;
  variableChildren: boolean;
  variables?: (Variable)[] | null;
  vectors?: (Vector)[] | null;
}
export interface DatasetProperty {
  dataset: Dataset;
  id: number;
  name: string;
  type: string;
  value: string;
}
export interface Variable {
  attributes: Attributes;
  dsgId: boolean;
  geoAxisX: GeoAxisX;
  geoAxisY: GeoAxisY;
  geometry: string;
  hash: string;
  id: number;
  intervals: string;
  name: string;
  subset: boolean;
  timeAxis: TimeAxis;
  title: string;
  type: string;
  units: string;
  url: string;
  variableAttributes?: (null)[] | null;
  variableProperties?: (VariableProperty | null)[] | null;
  verticalAxis?: null;
}
export interface Attributes {
}
export interface GeoAxisX {
  delta: number;
  dimensions: number;
  id: number;
  max: number;
  min: number;
  name: string;
  regular: boolean;
  size: number;
  title: string;
  type: string;
  units: string;
}
export interface GeoAxisY {
  delta: number;
  dimensions: number;
  id: number;
  max: number;
  min: number;
  name: string;
  regular: boolean;
  size: number;
  title: string;
  type: string;
  units: string;
}
export interface VerticalAxis {
  delta: number;
  dimensions: number;
  id: number;
  max: number;
  min: number;
  name: string;
  regular: boolean;
  size: number;
  title: string;
  type: string;
  units: string;
  positive: string;
}
export interface TimeAxis {
  calendar: string;
  climatology: boolean;
  delta: string;
  end: string;
  id: number;
  name: string;
  nameValuePairs?: (null)[] | null;
  period: string;
  position: string;
  size: number;
  start: string;
  title: string;
  units: string;
}
export interface VariableProperty {
  id: number;
  name: string;
  type: string;
  value: string;
  variable: Variable;
}
export interface Vector {
  attributes: Attributes;
  geometry: string;
  hash: string;
  id: number;
  name: string;
  title: string;
  type: string;
  u: Variable;
  v: Variable;
}
