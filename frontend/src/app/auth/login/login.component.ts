import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {

  email = '';
  password = '';
  error = '';

  constructor(private authService: AuthService, private router: Router) {}

  login() {
    this.authService.login(this.email, this.password).subscribe({
      next: (res) => {
        // ✅ Sauvegarder l'email
        this.authService.saveTempEmail(this.email);
        
        // ✅ Vérifier si MFA est configuré
        if (res.mfaConfigured) {
          // ✅ MFA déjà configuré → aller directement à /mfa
          this.router.navigate(['/mfa']);
        } else {
          // ✅ MFA PAS configuré → aller à /setup-mfa pour configurer
          this.router.navigate(['/setup-mfa']);
        }
      },
      error: () => {
        this.error = 'Email ou mot de passe incorrect';
      }
    });
  }
}