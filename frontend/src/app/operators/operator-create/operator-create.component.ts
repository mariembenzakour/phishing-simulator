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
  templateUrl: './operator-create.component.html',
  styleUrl: './operator-create.component.scss'
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

  // ✅ NOUVEAU : gestion de l'avatar
  avatarFile: File | null = null;
  avatarPreview: string | ArrayBuffer | null = null;
  avatarError = '';
  private readonly allowedAvatarTypes = ['image/jpeg', 'image/png', 'image/webp'];
  private readonly maxAvatarSize = 5 * 1024 * 1024; // 5 Mo

  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  // ✅ NOUVEAU : déclenché à la sélection d'un fichier
  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;

    if (!file) {
      return;
    }

    if (!this.allowedAvatarTypes.includes(file.type)) {
      this.avatarError = 'Formats acceptés : JPEG, PNG ou WEBP';
      input.value = '';
      return;
    }

    if (file.size > this.maxAvatarSize) {
      this.avatarError = 'L\'image ne doit pas dépasser 5 Mo';
      input.value = '';
      return;
    }

    this.avatarError = '';
    this.avatarFile = file;

    const reader = new FileReader();
    reader.onload = () => {
      this.avatarPreview = reader.result;
    };
    reader.readAsDataURL(file);
  }

  // ✅ NOUVEAU : retire l'avatar sélectionné
  removeAvatar(): void {
    this.avatarFile = null;
    this.avatarPreview = null;
    this.avatarError = '';
  }

  create() {
    // Validation des champs obligatoires
    if (!this.form.email || !this.form.password || !this.form.firstName || !this.form.lastName) {
      this.error = 'Tous les champs sont obligatoires';
      return;
    }

    // Validation du domaine email @intellisec.com
    if (!this.form.email.endsWith('@intellisec.com')) {
      this.error = 'L\'email doit être un email @intellisec.com';
      return;
    }

    // Validation complexité du mot de passe
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!passwordRegex.test(this.form.password)) {
      this.error = 'Min. 8 caractères avec majuscule, minuscule, chiffre et symbole';
      return;
    }

    this.authService.createOperator(this.form, this.avatarFile).subscribe({
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