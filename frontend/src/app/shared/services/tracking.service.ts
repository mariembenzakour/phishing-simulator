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

  // ── DASHBOARD ─────────────────────────────────────
  getGlobalDashboard(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/dashboard/global`);
  }

  getCampaignDetails(campaignId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/campaign/${campaignId}/details`);
  }

  // ============================================================
  // ✅ TIME-TO-CLICK (Option D - Complète)
  // ============================================================

  // 1. Time-to-click global de la campagne
  getTimeToClick(campaignId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/campaign/${campaignId}/time-to-click`);
  }

  // 2. Time-to-click par utilisateur
  getUserTimeToClick(targetId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/user/${targetId}/time-to-click`);
  }

  // 3. Time-to-click pour tous les utilisateurs d'une campagne
  getUsersTimeToClick(campaignId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/campaign/${campaignId}/users/time-to-click`);
  }

  // ── EXISTANT ──────────────────────────────────────
  getUserStats(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/user/stats`);
  }

  getUserHistory(targetId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/user/${targetId}/history`);
  }
}