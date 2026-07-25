import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = 'http://localhost:8086/api/auth';
  private tokenKey = 'token';
  private roleKey = 'role';
  private tempEmailKey = 'temp_email';

  constructor(private http: HttpClient) {}

  // ============================================
  // AUTHENTIFICATION
  // ============================================

  login(email: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, { email, password });
  }

  register(email: string, password: string, role: string,
           firstName: string, lastName: string,
           phone: string, birthDate: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, {
      email, password, role, firstName, lastName, phone, birthDate
    });
  }

  enableMfa(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/mfa/enable`, null, {
      params: { email },
      responseType: 'text'
    });
  }

  verifyMfa(email: string, code: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/mfa/verify`, null, {
      params: { email, code }
    });
  }

  createOperator(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/create-operator`, data);
  }

  deleteOperator(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/admin/delete-operator/${id}`);
  }

  getAllOperators(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/operators`);
  }

  // ============================================
  // GESTION DU TOKEN JWT
  // ============================================

  saveToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  // ============================================
  // GESTION DU RÔLE
  // ============================================

  saveRole(role: string): void {
    localStorage.setItem(this.roleKey, role);
  }

  getRole(): string | null {
    const savedRole = localStorage.getItem(this.roleKey);
    if (savedRole) return savedRole;

    const userInfo = this.getUserInfo();
    return userInfo?.role || null;
  }

  // ✅ MODIFICATION : Ajout de l'ID dans getUserInfo
  getUserInfo(): any {
    const token = this.getToken();
    if (!token) return null;
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload));
      return {
        email: decoded.sub,
        role: decoded.role,
        exp: decoded.exp,
        id: decoded.id || null   // ✅ ID extrait du token
      };
    } catch {
      return null;
    }
  }

  // ✅ NOUVELLE MÉTHODE : Récupérer l'ID de l'utilisateur
  getUserId(): string | null {
    return this.getUserInfo()?.id || null;
  }

  // ============================================
  // VÉRIFICATIONS DE RÔLE (RBAC)
  // ============================================

  isSuperAdmin(): boolean {
    return this.getRole() === 'SUPER_ADMIN';
  }

  isAdmin(): boolean {
    const role = this.getRole();
    return role === 'ADMIN' || role === 'SUPER_ADMIN';
  }

  isOperator(): boolean {
    return this.getRole() === 'OPERATOR';
  }

  isViewer(): boolean {
    return this.getRole() === 'VIEWER';
  }

  canCreateAdmin(): boolean {
    return this.isSuperAdmin();
  }

  canDeleteAdmin(): boolean {
    return this.isSuperAdmin();
  }

  hasRole(...roles: string[]): boolean {
    const currentRole = this.getRole();
    return roles.includes(currentRole || '');
  }

  hasAnyRole(roles: string[]): boolean {
    const currentRole = this.getRole();
    return roles.some(r => r === currentRole);
  }

  // ============================================
  // SESSION
  // ============================================

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) return false;

    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload));
      if (decoded.exp) {
        const now = Math.floor(Date.now() / 1000);
        if (decoded.exp < now) {
          this.logout();
          return false;
        }
      }
      return true;
    } catch {
      return false;
    }
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.roleKey);
    localStorage.removeItem(this.tempEmailKey);
  }

  // ============================================
  // EMAIL TEMPORAIRE (pour le flux MFA)
  // ============================================

  saveTempEmail(email: string): void {
    localStorage.setItem(this.tempEmailKey, email);
  }

  getTempEmail(): string | null {
    return localStorage.getItem(this.tempEmailKey);
  }

  clearTempEmail(): void {
    localStorage.removeItem(this.tempEmailKey);
  }
}