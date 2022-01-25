import {AddRequest} from "./add-request";
import {Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {Changes, UpdateSpec} from './app/application-state.service';
import {Injectable} from '@angular/core';

@Injectable()
export class AdminService {

  constructor(private httpClient: HttpClient) { }

  addDataset(addRequest: AddRequest): Observable<any> {
    return this.httpClient.post<any>('/las/admin/addDataset', addRequest);
  }
  saveDataset(changes: Changes): Observable<any> {
    return this.httpClient.put<Map<string, Map<number, string>>>('/las/admin/saveDataset', changes);
  }
  saveDatasetUpdateSpec(updateSpec: UpdateSpec): Observable<any> {
    return this.httpClient.post<any>('/las/admin/saveDatasetUpdateSpec', updateSpec);
  }
  saveSite(changes): Observable<any> {
    return this.httpClient.put<any>('/las/admin/saveSite', changes);
  }
  moveDataset(moveRequest: AddRequest): Observable<any> {
    return this.httpClient.post<any>('/las/admin/moveDataset', moveRequest);
  }
  deleteDataset(id: number): Observable<any> {
    return this.httpClient.delete<any>('/las/admin/deleteDataset/' + id);
  }
  deleteVariable(id: number): Observable<any> {
    return this.httpClient.delete<any>('/las/admin/deleteVariable/' + id);
  }
  deleteVector(id: number): Observable<any> {
    return this.httpClient.delete<any>('/las/admin/deleteVector/' + id);
  }
  addVectors(id: number): Observable<any> {
    return this.httpClient.get('/las/admin/addVectors/' + id);
  }
  listBackups(): Observable<any> {
    return this.httpClient.get('/las/admin/listBackups');
  }
  backup(): Observable<any> {
    return this.httpClient.get('/las/admin/backup');
  }
  restore(backup): Observable<any> {
    return this.httpClient.post<any>('/las/admin/restore', backup)
  }
  deleteBackup(backup): Observable<any> {
    return this.httpClient.post<any>('/las/admin/deleteBackup', backup)
  }
}
