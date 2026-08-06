import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TrackingService {

  private apiUrl = 'http://localhost:8086/track';
  private emailApiUrl = 'http://localhost:8086/api/email';

  constructor(private http: HttpClient) {}

  // ── EXISTANT ──────────────────────────────────────
  getCampaignEvents(campaignId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/campaign/${campaignId}`);
  }

  getSendEventTimeline(sendEventId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/events/${sendEventId}`);
  }

  getDeliverabilityStats(campaignId: string): Observable<any> {
    return this.http.get<any>(`${this.emailApiUrl}/stats/${campaignId}`);
  }

  getGlobalStats(): Observable<any> {
    return this.http.get<any>(`${this.emailApiUrl}/stats/global`);
  }

  // ✅ NOUVEAU : Dashboard global
  getGlobalDashboard(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/dashboard/global`);
  }

  // ✅ NOUVEAU : Détails d'une campagne
  getCampaignDetails(campaignId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/campaign/${campaignId}/details`);
  }

  // ✅ NOUVEAU : Statistiques par utilisateur
  getUserStats(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/user/stats`);
  }

  // ✅ NOUVEAU : Historique d'un utilisateur
  getUserHistory(targetId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/user/${targetId}/history`);
  }
}