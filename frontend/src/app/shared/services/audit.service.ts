import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AuditLog {
  id: string;
  action: string;
  actor: string;
  target: string | null;
  details: string;
  previousHash: string;
  hash: string;
  createdAt: string;
}

export interface AuditStats {
  totalLogs: number;
  totalSends: number;
  sendsLast24h: number;
  sendsLast7Days: number;
  recentSends: AuditLog[];
}

export interface IntegrityResult {
  valid: boolean;
  message: string;
  totalLogs: number;
  checked: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private apiUrl = 'http://localhost:8086/api/audit';

  constructor(private http: HttpClient) {}

  getLogs(): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.apiUrl}/logs`);
  }

  getLogsByAction(action: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.apiUrl}/action`, { params: { action } });
  }

  getLogsByActor(actor: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.apiUrl}/actor`, { params: { actor } });
  }

  verifyIntegrity(): Observable<IntegrityResult> {
    return this.http.get<IntegrityResult>(`${this.apiUrl}/verify`);
  }

  getStats(): Observable<AuditStats> {
    return this.http.get<AuditStats>(`${this.apiUrl}/stats`);
  }
}