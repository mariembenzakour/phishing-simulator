import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../shared/navbar/navbar.component';
import { AiService, GenerationRequest, GenerationResponse, RedFlag } from '../shared/services/ai.service';
import { AuthService } from '../shared/services/auth.service';

@Component({
  selector: 'app-ai-generation',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './ai-generation.component.html'
})
export class AiGenerationComponent {

  form: GenerationRequest = {
    scenario: '',
    department: 'IT',
    language: 'fr',
    urgency: 'medium',
    additionalDetails: ''
  };

  result: GenerationResponse | null = null;
  loading = false;
  error = '';
  success = '';
  generationId: string | null = null;

  // ✅ WEEK 6 : Onglets
  activeTab: 'email' | 'landing' | 'redflags' | 'preview' = 'email';

  // ✅ WEEK 6 : Red flags parsés
  redFlags: RedFlag[] = [];

  scenarios = [
    'Mot de passe expiré',
    'Alerte de sécurité',
    'Connexion suspecte',
    'Mise à jour de sécurité',
    'Demande d\'action RH',
    'Facture en attente',
    'Confirmation de compte',
    'Phishing simulation'
  ];

  departments = ['IT', 'RH', 'Direction', 'Comptabilité', 'Marketing', 'Commercial', 'Général'];

  languages = [
    { value: 'fr', label: 'Français (Québec)' },
    { value: 'en', label: 'English' }
  ];

  urgencies = [
    { value: 'low', label: '🟢 Basse' },
    { value: 'medium', label: '🟠 Moyenne' },
    { value: 'high', label: '🔴 Haute' }
  ];

  constructor(
    private aiService: AiService,
    private authService: AuthService,
    private router: Router
  ) {}

  generate() {
    if (!this.form.scenario) {
      this.error = 'Veuillez saisir un scénario';
      return;
    }

    this.loading = true;
    this.error = '';
    this.success = '';
    this.result = null;
    this.generationId = null;
    this.redFlags = [];
    this.activeTab = 'email';

    this.aiService.generate(this.form).subscribe({
      next: (response) => {
        this.result = response;
        this.loading = false;
        this.success = '✅ Email généré avec succès !';
        
        // ✅ WEEK 6 : Parser les red flags
        if (response.redFlags) {
          this.redFlags = this.aiService.parseRedFlags(response.redFlags);
        }
        
        if (response.id) {
          this.generationId = response.id;
        }
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors de la génération';
        this.loading = false;
      }
    });
  }

  // ✅ WEEK 6 : Changer d'onglet
  setActiveTab(tab: 'email' | 'landing' | 'redflags' | 'preview') {
    this.activeTab = tab;
  }

  approve() {
    if (!this.generationId) {
      this.error = 'Aucun draft à approuver. Veuillez d\'abord générer un email.';
      return;
    }

    this.loading = true;
    this.aiService.approve(this.generationId).subscribe({
      next: (response) => {
        this.result = response;
        this.loading = false;
        this.success = '✅ Draft approuvé avec succès !';
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors de l\'approbation';
        this.loading = false;
      }
    });
  }

  reject() {
    if (!this.generationId) {
      this.error = 'Aucun draft à rejeter';
      return;
    }

    if (!confirm('Êtes-vous sûr de vouloir rejeter ce draft ?')) {
      return;
    }

    this.loading = true;
    this.aiService.reject(this.generationId).subscribe({
      next: () => {
        this.loading = false;
        this.success = '❌ Draft rejeté';
        this.result = null;
        this.generationId = null;
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors du rejet';
        this.loading = false;
      }
    });
  }

  reset() {
    this.result = null;
    this.error = '';
    this.success = '';
    this.generationId = null;
    this.redFlags = [];
    this.activeTab = 'email';
  }

  getStatusColor(status: string): string {
    const map: Record<string, string> = {
      'DRAFT': '#f59e0b',
      'APPROVED': '#22c55e'
    };
    return map[status] || '#6b7280';
  }

  getLanguageLabel(lang: string): string {
    const map: Record<string, string> = {
      'fr': 'Français',
      'en': 'English'
    };
    return map[lang] || lang;
  }

  isAdmin(): boolean {
    return this.authService.isAdmin() || this.authService.isSuperAdmin();
  }

  // ✅ WEEK 6 : Obtenir la couleur de sévérité
  getSeverityColor(severity: string): string {
    return this.aiService.getSeverityColor(severity);
  }

  // ✅ WEEK 6 : Obtenir l'icône de sévérité
  getSeverityIcon(severity: string): string {
    return this.aiService.getSeverityIcon(severity);
  }

  // ✅ WEEK 6 : Vérifier si un onglet est actif
  isActiveTab(tab: string): boolean {
    return this.activeTab === tab;
  }
}