import {HttpClient} from '@angular/common/http';
import {Dataset} from './app/json/Dataset';
import {Site} from './app/json/Site';
import {Injectable} from '@angular/core';

@Injectable()
export class DatasetService {

  constructor(private httpClient: HttpClient) { }
  getSite() {
    return this.httpClient.get<Site>('/las/site/show/first.json');
  }
  getPrivate() {
    return this.httpClient.get<Site>('/las/site/show/2.json');
  }
  getDataset(id: number) {
    return this.httpClient.get<Dataset>('/las/dataset/show/'+id+".json");
  }
}
