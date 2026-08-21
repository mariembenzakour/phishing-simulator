import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface GenerationRequest {
  scenario: string;
  department?: string;
  language: string;
  urgency?: string;
  additionalDetails?: string;
}

export interface GenerationResponse {
  id?: string;
  subject: string;
  bodyHtml: string;
  bodyText: string;
  senderName?: string;        // ✅ WEEK 6
  senderDomain?: string;       // ✅ WEEK 6
  landingPageHtml?: string;    // ✅ WEEK 6
  redFlags?: string;           // ✅ WEEK 6 (JSON string)
  language: string;
  scenario: string;
  status: string;
  approved: boolean;
  approvedBy?: string;
  generatedAt: string;
}

export interface RedFlag {
  type: string;
  title: string;
  description: string;
  severity: 'critical' | 'high' | 'medium' | 'low';
  howToDetect: string;
}

export interface AiGenerationLog {
  id: string;
  scenario: string;
  language: string;
  prompt: string;
  generatedSubject: string;
  generatedBody: string;
  bodyText: string;
  landingPageHtml?: string;   // ✅ WEEK 6
  redFlags?: string;          // ✅ WEEK 6
  senderName?: string;        // ✅ WEEK 6
  senderDomain?: string;      // ✅ WEEK 6
  generatedBy: string;
  approvedBy?: string;
  approved: boolean;
  createdAt: string;
  approvedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class AiService {

  private apiUrl = 'http://localhost:8086/api/ai';

  constructor(private http: HttpClient) {}

  generate(request: GenerationRequest): Observable<GenerationResponse> {
    return this.http.post<GenerationResponse>(`${this.apiUrl}/generate`, request);
  }

  approve(id: string): Observable<GenerationResponse> {
    return this.http.put<GenerationResponse>(`${this.apiUrl}/approve/${id}`, {});
  }

  reject(id: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/reject/${id}`, {});
  }

  getDrafts(): Observable<{ total: number; drafts: AiGenerationLog[] }> {
    return this.http.get<{ total: number; drafts: AiGenerationLog[] }>(`${this.apiUrl}/drafts`);
  }

  getApprovedDrafts(): Observable<{ total: number; drafts: AiGenerationLog[] }> {
    return this.http.get<{ total: number; drafts: AiGenerationLog[] }>(`${this.apiUrl}/approved`);
  }

  getDraft(id: string): Observable<AiGenerationLog> {
    return this.http.get<AiGenerationLog>(`${this.apiUrl}/draft/${id}`);
  }

  getStatus(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/status`);
  }

  // ✅ WEEK 6 : Parser les red flags
  parseRedFlags(redFlagsJson: string): RedFlag[] {
    try {
      return JSON.parse(redFlagsJson);
    } catch {
      return [];
    }
  }

  // ✅ WEEK 6 : Obtenir la couleur selon la sévérité
  getSeverityColor(severity: string): string {
    const map: Record<string, string> = {
      'critical': '#dc2626',
      'high': '#f59e0b',
      'medium': '#f97316',
      'low': '#22c55e'
    };
    return map[severity] || '#6b7280';
  }

  // ✅ WEEK 6 : Obtenir l'icône selon la sévérité
  getSeverityIcon(severity: string): string {
    const map: Record<string, string> = {
      'critical': '🔴',
      'high': '🟠',
      'medium': '🟡',
      'low': '🟢'
    };
    return map[severity] || '⚪';
  }
}