import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Vérifier si l'utilisateur est connecté
  if (!authService.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }

  // Vérifier le rôle si spécifié dans la route
  const requiredRoles = route.data?.['roles'] as string[];
  if (requiredRoles && requiredRoles.length > 0) {
    const userRole = authService.getRole();
    
    // ✅ SUPER_ADMIN a accès à TOUT
    if (userRole === 'SUPER_ADMIN') {
      return true;
    }
    
    // Vérifier si l'utilisateur a l'un des rôles requis
    if (!requiredRoles.includes(userRole!)) {
      router.navigate(['/unauthorized']);
      return false;
    }
  }
 
  return true;
};