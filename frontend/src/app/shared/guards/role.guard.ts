import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const requiredRoles = route.data['roles'] as string[];
    
    // Si aucun rôle requis, autoriser
    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }

    // ✅ SUPER_ADMIN a accès à TOUT (même si son rôle n'est pas dans la liste)
    if (this.authService.isSuperAdmin()) {
      return true;
    }

    // Vérifier si l'utilisateur a l'un des rôles requis
    const hasRole = this.authService.hasAnyRole(requiredRoles);
    
    if (!hasRole) {
      // ✅ Rediriger vers la page unauthorized
      this.router.navigate(['/unauthorized']);
      return false;
    }

    return true;
  }
}