import {AddProperty} from "./add-property";

export interface AddRequest {
  url: string;  // File name or url
  type: string; // One of netcdf, thredds, erddap, dsg
  addProperties: AddProperty[]
}
