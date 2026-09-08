import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent implements OnInit {

  isCollapsed = false;

  // ✅ NOUVEAU : URL complète de l'avatar de l'utilisateur connecté (absent du JWT)
  avatarUrl: string | null = null;

  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
  }

  // ✅ NOUVEAU : va chercher le profil complet (avatar inclus) auprès du backend
  private loadCurrentUser(): void {
    if (!this.authService.isLoggedIn()) {
      return;
    }

    this.authService.getCurrentUser().subscribe({
      next: (user) => {
        this.avatarUrl = this.authService.getAvatarUrl(user?.avatar);
      },
      error: () => {
        this.avatarUrl = null;
      }
    });
  }

  toggleSidebar() {
    this.isCollapsed = !this.isCollapsed;
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}