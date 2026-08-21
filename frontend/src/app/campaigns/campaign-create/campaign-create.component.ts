import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { CampaignService } from '../../shared/services/campaign.service';
import { TargetGroupService } from '../../shared/services/target-group.service';
import { SenderProfileService } from '../../shared/services/sender-profile.service';
import { AuthService } from '../../shared/services/auth.service';
import { AiService, AiGenerationLog } from '../../shared/services/ai.service';

@Component({
  selector: 'app-campaign-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './campaign-create.component.html'
})
export class CampaignCreateComponent implements OnInit {

  isEditMode = false;
  campaignId: string = '';

  form = {
    name: '',
    senderEmail: '',
    senderProfileId: '',
    targetGroupId: '',
    scheduledAt: '',
    dryRun: false,
    dryRunEmail: '',
    throttleSeconds: 5,
    status: 'DRAFT',
    aiGenerationId: '',
    customSubject: '',
    customBodyHtml: '',
    customBodyText: ''
  };

  groups: any[] = [];
  senderProfiles: any[] = [];
  aiDrafts: AiGenerationLog[] = [];
  loadingAiDrafts = false;
  error = '';
  success = '';
  loading = false;

  constructor(
    private campaignService: CampaignService,
    private groupService: TargetGroupService,
    private senderProfileService: SenderProfileService,
    private aiService: AiService,
    public router: Router,
    private route: ActivatedRoute,
    public authService: AuthService
  ) {}

  ngOnInit() {
    this.groupService.getAll().subscribe({
      next: (data) => {
        this.groups = data;
        if (data.length > 0 && !this.isEditMode) this.form.targetGroupId = data[0].id;
      },
      error: () => { this.error = 'Erreur lors du chargement des groupes'; }
    });

    this.senderProfileService.getAll().subscribe({
      next: (data) => { this.senderProfiles = data; },
      error: () => { console.error('Erreur chargement profils'); }
    });

    this.loadAiDrafts();

    const id = this.route.snapshot.params['id'];
    if (id) {
      this.isEditMode = true;
      this.campaignId = id;
      this.loadCampaign(id);
    }
  }

  /**
   * ✅ Charge les drafts IA approuvés (utilisables dans les campagnes)
   * Utilise l'endpoint /api/ai/approved pour récupérer uniquement les drafts approuvés
   */
  loadAiDrafts() {
    this.loadingAiDrafts = true;
    
    // ✅ Appel au nouvel endpoint /api/ai/approved
    this.aiService.getApprovedDrafts().subscribe({
      next: (data) => {
        console.log('📋 Drafts approuvés reçus:', data);
        this.aiDrafts = data.drafts || [];
        console.log('✅ Drafts disponibles:', this.aiDrafts);
        this.loadingAiDrafts = false;
      },
      error: (err) => {
        this.loadingAiDrafts = false;
        console.error('❌ Erreur chargement drafts IA:', err);
        // Fallback: essayer avec l'ancien endpoint et filtrer
        this.loadAiDraftsFallback();
      }
    });
  }

  /**
   * ✅ Fallback : charger tous les drafts et filtrer les approuvés
   */
  private loadAiDraftsFallback() {
    this.aiService.getDrafts().subscribe({
      next: (data) => {
        console.log('📋 Fallback - Drafts reçus:', data);
        this.aiDrafts = data.drafts.filter(d => d.approved === true);
        console.log('✅ Drafts approuvés (fallback):', this.aiDrafts);
        this.loadingAiDrafts = false;
      },
      error: (err) => {
        this.loadingAiDrafts = false;
        console.error('❌ Erreur fallback:', err);
      }
    });
  }

  onAiDraftChange() {
    if (!this.form.aiGenerationId) {
      this.form.customSubject = '';
      this.form.customBodyHtml = '';
      this.form.customBodyText = '';
      return;
    }

    const selected = this.aiDrafts.find(d => d.id === this.form.aiGenerationId);
    if (selected) {
      this.form.customSubject = selected.generatedSubject;
      this.form.customBodyHtml = selected.generatedBody;
      this.form.customBodyText = selected.bodyText;
      this.success = '✅ Contenu IA chargé automatiquement !';
      setTimeout(() => { this.success = ''; }, 3000);
    }
  }

  loadCampaign(id: string) {
    this.loading = true;
    this.campaignService.getById(id).subscribe({
      next: (data) => {
        this.form = {
          name: data.name || '',
          senderEmail: data.senderEmail || '',
          senderProfileId: data.senderProfileId || '',
          targetGroupId: data.targetGroupId || '',
          scheduledAt: data.scheduledAt ? new Date(data.scheduledAt).toISOString().slice(0, 16) : '',
          dryRun: data.dryRun || false,
          dryRunEmail: data.dryRunEmail || '',
          throttleSeconds: data.throttleSeconds || 5,
          status: data.status || 'DRAFT',
          aiGenerationId: data.aiGenerationId || '',
          customSubject: data.customSubject || '',
          customBodyHtml: data.customBodyHtml || '',
          customBodyText: data.customBodyText || ''
        };
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement de la campagne';
        this.loading = false;
      }
    });
  }

  create() {
    if (!this.form.name || !this.form.senderEmail || !this.form.targetGroupId) {
      this.error = 'Le nom, l\'email expéditeur et le groupe sont obligatoires';
      return;
    }

    if (this.form.dryRun && !this.form.dryRunEmail) {
      this.error = 'Veuillez renseigner un email de test pour le mode Dry Run';
      return;
    }

    const payload: any = {
      name: this.form.name,
      senderEmail: this.form.senderEmail,
      senderProfileId: this.form.senderProfileId || null,
      targetGroupId: this.form.targetGroupId,
      scheduledAt: this.form.scheduledAt ? new Date(this.form.scheduledAt).toISOString() : null,
      dryRun: this.form.dryRun,
      dryRunEmail: this.form.dryRunEmail || null,
      throttleSeconds: this.form.throttleSeconds || 5,
      status: this.form.status,
      aiGenerationId: this.form.aiGenerationId || null,
      customSubject: this.form.customSubject || null,
      customBodyHtml: this.form.customBodyHtml || null,
      customBodyText: this.form.customBodyText || null
    };

    if (this.isEditMode) {
      this.campaignService.update(this.campaignId, payload).subscribe({
        next: () => {
          this.success = 'Campagne mise à jour avec succès !';
          setTimeout(() => this.router.navigate(['/campaigns']), 1500);
        },
        error: (err: any) => {
          this.error = err.error?.message || 'Erreur lors de la mise à jour';
        }
      });
    } else {
      this.campaignService.create(payload).subscribe({
        next: () => {
          this.success = 'Campagne créée avec succès !';
          setTimeout(() => this.router.navigate(['/campaigns']), 1500);
        },
        error: () => {
          this.error = 'Erreur lors de la création';
        }
      });
    }
  }

  getTitle(): string {
    return this.isEditMode ? '✏️ Modifier la campagne' : '➕ Nouvelle campagne';
  }

  getButtonText(): string {
    return this.isEditMode ? '💾 Mettre à jour' : '🚀 CRÉER LA CAMPAGNE';
  }
}