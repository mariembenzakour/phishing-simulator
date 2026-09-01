import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-mfa',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './mfa.component.html',
  styleUrl: './mfa.component.scss' // 👈 Liaison du fichier SCSS
})
export class MfaComponent {

  code = '';
  error = '';

  constructor(private authService: AuthService, private router: Router) {}

  verify() {
    const email = this.authService.getTempEmail();
    if (!email) {
      this.router.navigate(['/login']);
      return;
    }

    this.authService.verifyMfa(email, parseInt(this.code)).subscribe({
      next: (res) => {
        // ✅ Sauvegarder le token
        this.authService.saveToken(res.token);
        
        // ✅ Si l'opérateur est présent, sauvegarder le rôle
        if (res.operator) {
          this.authService.saveRole(res.operator.role);
        }
        
        // ✅ Nettoyer l'email temporaire
        this.authService.clearTempEmail();
        
        // ✅ Rediriger vers les campagnes
        this.router.navigate(['/campaigns']);
      },
      error: () => {
        this.error = 'Code invalide. Réessaie.';
      }
    });
  }
}