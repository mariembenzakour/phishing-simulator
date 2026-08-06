import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CampaignService {

  private apiUrl = 'http://localhost:8086/api/campaigns';
  private emailApiUrl = 'http://localhost:8086/api/email';

  constructor(private http: HttpClient) {}

  getAll(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  getById(id: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}`);
  }

  create(campaign: any): Observable<any> {
    return this.http.post(this.apiUrl, campaign);
  }

  // ✅ NOUVEAU : Mettre à jour une campagne
  update(id: string, campaign: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, campaign);
  }

  clone(id: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/clone`, {});
  }

  authorize(id: string, operatorId: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/authorize`, null, {
      params: { operatorId }
    });
  }

  pause(id: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/pause`, {});
  }

  resume(id: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/resume`, {});
  }

  delete(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  sendCampaign(id: string): Observable<string> {
    return this.http.post(`${this.emailApiUrl}/send-campaign/${id}`, {}, { responseType: 'text' });
  }

  sendTestEmail(to: string, subject: string, body: string): Observable<string> {
    return this.http.post(`${this.emailApiUrl}/test`, { to, subject, body }, { responseType: 'text' });
  }
}