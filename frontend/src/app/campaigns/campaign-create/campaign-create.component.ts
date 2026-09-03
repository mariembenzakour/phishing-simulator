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

export interface DeliverabilityResult {
  score: number;
  statusText: string;
  summary: string;
  dnsSpf: boolean;
  spamKeywordsScore: number;
  spamKeywordsCount: number;
  htmlRatioValid: boolean;
  isBlacklisted: boolean;
  recommendations: string[];
}

@Component({
  selector: 'app-campaign-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './campaign-create.component.html',
  styleUrl: './campaign-create.component.scss'
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
    customBodyText: '',
    allowList: ''
  };

  groups: any[] = [];
  senderProfiles: any[] = [];
  aiDrafts: AiGenerationLog[] = [];
  loadingAiDrafts = false;
  error = '';
  success = '';
  loading = false;

  // ÉTATS DÉLIVRABILITÉ
  testingDeliverability = false;
  deliverabilityResult: DeliverabilityResult | null = null;

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

  loadAiDrafts() {
    this.loadingAiDrafts = true;
    this.aiService.getApprovedDrafts().subscribe({
      next: (data) => {
        this.aiDrafts = data.drafts || [];
        this.loadingAiDrafts = false;
      },
      error: () => {
        this.loadAiDraftsFallback();
      }
    });
  }

  private loadAiDraftsFallback() {
    this.aiService.getDrafts().subscribe({
      next: (data) => {
        this.aiDrafts = data.drafts.filter(d => d.approved === true);
        this.loadingAiDrafts = false;
      },
      error: () => {
        this.loadingAiDrafts = false;
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
      this.success = 'Contenu IA appliqué automatiquement à la campagne.';
      setTimeout(() => { this.success = ''; }, 3000);
    }
  }

  // ✅ TEST DÉLIVRABILITÉ - EXÉCUTION HTTP DEPUIS LE BACKEND
  testDeliverability() {
    if (!this.form.senderEmail) {
      this.error = 'Veuillez renseigner l\'email expéditeur avant de lancer le test.';
      return;
    }

    this.testingDeliverability = true;
    this.error = '';

    const payload = {
      senderEmail: this.form.senderEmail,
      subject: this.form.customSubject,
      bodyHtml: this.form.customBodyHtml,
      bodyText: this.form.customBodyText
    };

    this.campaignService.checkDeliverability(payload).subscribe({
      next: (result) => {
        this.deliverabilityResult = result;
        this.testingDeliverability = false;
      },
      error: (err) => {
        this.error = 'Erreur lors de la vérification de délivrabilité sur le serveur.';
        this.testingDeliverability = false;
      }
    });
  }

  getScoreClass(score: number): string {
    if (score >= 80) return 'score-good';
    if (score >= 50) return 'score-warning';
    return 'score-danger';
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
          customBodyText: data.customBodyText || '',
          allowList: data.allowList || ''
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
      this.error = 'Le nom de la campagne, l\'email expéditeur et le groupe de cibles sont obligatoires.';
      return;
    }

    if (this.form.dryRun && !this.form.dryRunEmail) {
      this.error = 'Veuillez préciser un email de test pour le mode Dry Run.';
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
      customBodyText: this.form.customBodyText || null,
      allowList: this.form.allowList || null
    };

    if (this.isEditMode) {
      this.campaignService.update(this.campaignId, payload).subscribe({
        next: () => {
          this.success = 'Campagne mise à jour avec succès.';
          setTimeout(() => this.router.navigate(['/campaigns']), 1200);
        },
        error: (err: any) => {
          this.error = err.error?.message || 'Erreur lors de la mise à jour de la campagne.';
        }
      });
    } else {
      this.campaignService.create(payload).subscribe({
        next: () => {
          this.success = 'Campagne créée avec succès.';
          setTimeout(() => this.router.navigate(['/campaigns']), 1200);
        },
        error: () => {
          this.error = 'Erreur lors de la création de la campagne.';
        }
      });
    }
  }

  getTitle(): string {
    return this.isEditMode ? 'Modifier la campagne' : 'Nouvelle campagne';
  }

  getButtonText(): string {
    return this.isEditMode ? 'Mettre à jour la campagne' : 'Créer la campagne';
  }
}