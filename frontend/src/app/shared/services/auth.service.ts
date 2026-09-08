import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  // ✅ NOUVEAU : base séparée de apiUrl pour pouvoir reconstruire l'URL complète des avatars
  private baseUrl = 'http://localhost:8086';
  private apiUrl = `${this.baseUrl}/api/auth`;
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

  // ✅ MODIFIÉ : envoi en multipart/form-data pour supporter l'upload d'avatar
  register(email: string, password: string, role: string,
           firstName: string, lastName: string,
           phone: string, birthDate: string, avatar?: File | null): Observable<any> {

    const data = { email, password, role, firstName, lastName, phone, birthDate };
    const formData = this.buildOperatorFormData(data, avatar);

    // ⚠️ Ne pas fixer manuellement le header Content-Type : le navigateur
    // ajoute automatiquement le bon "boundary" pour le multipart.
    return this.http.post(`${this.apiUrl}/register`, formData);
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

  // ✅ MODIFIÉ : envoi en multipart/form-data pour supporter l'upload d'avatar
  createOperator(data: any, avatar?: File | null): Observable<any> {
    const formData = this.buildOperatorFormData(data, avatar);
    return this.http.post(`${this.apiUrl}/admin/create-operator`, formData);
  }

  deleteOperator(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/admin/delete-operator/${id}`);
  }

  getAllOperators(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/operators`);
  }

  // ✅ NOUVEAU : profil complet de l'utilisateur connecté (contient l'avatar, absent du JWT)
  getCurrentUser(): Observable<any> {
    return this.http.get(`${this.apiUrl}/me`);
  }

  // ✅ NOUVEAU : construit l'URL absolue d'un avatar à partir du chemin relatif renvoyé par le backend
  getAvatarUrl(avatarPath: string | null | undefined): string | null {
    if (!avatarPath) {
      return null;
    }
    return avatarPath.startsWith('http') ? avatarPath : `${this.baseUrl}${avatarPath}`;
  }

  // ✅ NOUVEAU : helper commun pour construire le FormData (JSON "data" + fichier "avatar")
  private buildOperatorFormData(data: any, avatar?: File | null): FormData {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));

    if (avatar) {
      formData.append('avatar', avatar, avatar.name);
    }

    return formData;
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
        id: decoded.id || null
      };
    } catch {
      return null;
    }
  }

  getUserId(): string | null {
    return this.getUserInfo()?.id || null;
  }

  // ============================================
  // ✅ VÉRIFICATIONS DE RÔLE (RBAC) - CORRIGÉ
  // ============================================

  isSuperAdmin(): boolean {
    return this.getRole() === 'SUPER_ADMIN';
  }

  // ✅ isAdmin() retourne TRUE pour ADMIN et SUPER_ADMIN
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

  hasRole(role: string): boolean {
    const currentRole = this.getRole();
    return currentRole === role;
  }

  // ✅ SUPER_ADMIN a TOUJOURS accès à tout
  hasAnyRole(roles: string[]): boolean {
    const currentRole = this.getRole();
    return roles.some(r => r === currentRole) || this.isSuperAdmin();
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