import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TrackingService {

  private apiUrl = 'http://localhost:8086/track';
  private emailApiUrl = 'http://localhost:8086/api/email';

  constructor(private http: HttpClient) {}

  getCampaignEvents(campaignId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/campaign/${campaignId}`);
  }

  getSendEventTimeline(sendEventId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/events/${sendEventId}`);
  }

  // ✅ NOUVEAU : Récupérer les statistiques de délivrabilité
  getDeliverabilityStats(campaignId: string): Observable<any> {
    return this.http.get<any>(`${this.emailApiUrl}/stats/${campaignId}`);
  }

  // ✅ NOUVEAU : Récupérer les statistiques globales
  getGlobalStats(): Observable<any> {
    return this.http.get<any>(`${this.emailApiUrl}/stats/global`);
  }
}