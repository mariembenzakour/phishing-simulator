import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TargetService {

  private apiUrl = 'http://localhost:8086/api/targets';

  constructor(private http: HttpClient) {}

  getByGroup(groupId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/group/${groupId}`);
  }

  create(target: any): Observable<any> {
    return this.http.post(this.apiUrl, target);
  }

  delete(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  // ✅ NOUVELLE METHODE : Upload CSV
  uploadCsv(file: File, groupId: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('groupId', groupId);
    
    return this.http.post(`${this.apiUrl}/upload`, formData);
  }
}