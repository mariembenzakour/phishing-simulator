import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SenderProfileService {

  private apiUrl = 'http://localhost:8086/api/sender-profiles';

  constructor(private http: HttpClient) {}

  getAll(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  create(profile: any): Observable<any> {
    return this.http.post(this.apiUrl, profile);
  }

  update(id: string, profile: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, profile);
  }

  delete(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}