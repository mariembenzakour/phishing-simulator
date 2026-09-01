import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TargetGroupService {

  private apiUrl = 'http://localhost:8086/api/groups';

  constructor(private http: HttpClient) {}

  getAll(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  getById(id: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  create(group: any): Observable<any> {
    return this.http.post(this.apiUrl, group);
  }

  update(id: string, group: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, group);
  }

  delete(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}