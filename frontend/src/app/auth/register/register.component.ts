import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html'
})
export class RegisterComponent {

  form = {
    firstName: '',
    lastName: '',
    birthDate: '',
    phone: '',
    email: '',
    password: '',
    confirmPassword: ''
    // ✅ PLUS DE ROLE ICI
  };

  errors: any = {};
  success = '';

  constructor(private authService: AuthService, private router: Router) {}

  validate(): boolean {
    this.errors = {};

    // Prénom
    if (!this.form.firstName.trim()) {
      this.errors.firstName = 'Le prénom est obligatoire';
    } else if (this.form.firstName.length < 2) {
      this.errors.firstName = 'Le prénom doit contenir au moins 2 caractères';
    }

    // Nom
    if (!this.form.lastName.trim()) {
      this.errors.lastName = 'Le nom est obligatoire';
    } else if (this.form.lastName.length < 2) {
      this.errors.lastName = 'Le nom doit contenir au moins 2 caractères';
    }

    // Date de naissance
    if (!this.form.birthDate) {
      this.errors.birthDate = 'La date de naissance est obligatoire';
    } else {
      const birth = new Date(this.form.birthDate);
      const today = new Date();
      const age = today.getFullYear() - birth.getFullYear();
      if (age < 18) {
        this.errors.birthDate = 'Vous devez avoir au moins 18 ans';
      }
    }

    // Téléphone
    const phoneRegex = /^[+]?[\d\s\-]{8,15}$/;
    if (!this.form.phone.trim()) {
      this.errors.phone = 'Le téléphone est obligatoire';
    } else if (!phoneRegex.test(this.form.phone)) {
      this.errors.phone = 'Numéro de téléphone invalide';
    }

    // Email — domaine intellisec.com obligatoire
    if (!this.form.email.trim()) {
      this.errors.email = 'L\'email est obligatoire';
    } else if (!this.form.email.endsWith('@intellisec.com')) {
      this.errors.email = 'L\'email doit être un email @intellisec.com';
    }

    // Mot de passe
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!this.form.password) {
      this.errors.password = 'Le mot de passe est obligatoire';
    } else if (!passwordRegex.test(this.form.password)) {
      this.errors.password = 'Min. 8 caractères avec majuscule, minuscule, chiffre et symbole';
    }

    // Confirmation mot de passe
    if (!this.form.confirmPassword) {
      this.errors.confirmPassword = 'Veuillez confirmer le mot de passe';
    } else if (this.form.password !== this.form.confirmPassword) {
      this.errors.confirmPassword = 'Les mots de passe ne correspondent pas';
    }

    return Object.keys(this.errors).length === 0;
  }

  register() {
    if (!this.validate()) return;

    // ✅ On envoie TOUJOURS VIEWER
    this.authService.register(
      this.form.email,
      this.form.password,
      'VIEWER',  // ← FORCÉ
      this.form.firstName,
      this.form.lastName,
      this.form.phone,
      this.form.birthDate
    ).subscribe({
      next: () => {
        this.authService.saveTempEmail(this.form.email);
        this.success = 'Compte créé ! Configuration du MFA...';
        setTimeout(() => {
          this.router.navigate(['/setup-mfa']);
        }, 1500);
      },
      error: () => {
        this.errors.general = 'Erreur lors de la création du compte';
      }
    });
  }
}