import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-setup-mfa',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './setup-mfa.component.html'
})
export class SetupMfaComponent implements OnInit {

  qrUrl = '';
  error = '';

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
    const email = this.authService.getTempEmail();
    if (!email) {
      this.router.navigate(['/login']);
      return;
    }

    this.authService.enableMfa(email).subscribe({
      next: (url: string) => {
        this.qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(url)}`;
      },
      error: () => {
        this.error = 'Erreur lors de la génération du QR code';
      }
    });
  }

  next() {
    this.router.navigate(['/mfa']);
  }
}