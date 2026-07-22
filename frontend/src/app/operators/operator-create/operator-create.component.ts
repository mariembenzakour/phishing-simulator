import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-operator-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './operator-create.component.html'
})
export class OperatorCreateComponent {

  form = {
    email: '',
    password: '',
    role: 'OPERATOR',
    firstName: '',
    lastName: '',
    phone: '',
    birthDate: ''
  };

  error = '';
  success = '';

  // ✅ Rendre authService PUBLIC pour l'utiliser dans le template HTML
  constructor(
    public authService: AuthService,  // ← CHANGÉ : private → public
    private router: Router
  ) {}

  create() {
    // Validation
    if (!this.form.email || !this.form.password || !this.form.firstName || !this.form.lastName) {
      this.error = 'Tous les champs sont obligatoires';
      return;
    }

    // Validation email @intellisec.com
    if (!this.form.email.endsWith('@intellisec.com')) {
      this.error = 'L\'email doit être un email @intellisec.com';
      return;
    }

    // Validation mot de passe
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!passwordRegex.test(this.form.password)) {
      this.error = 'Min. 8 caractères avec majuscule, minuscule, chiffre et symbole';
      return;
    }

    this.authService.createOperator(this.form).subscribe({
      next: () => {
        this.success = `Opérateur ${this.form.role} créé avec succès !`;
        setTimeout(() => {
          this.router.navigate(['/campaigns']);
        }, 1500);
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors de la création';
      }
    });
  }
}