import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { CampaignService } from '../../shared/services/campaign.service';
import { TargetGroupService } from '../../shared/services/target-group.service';
import { SenderProfileService } from '../../shared/services/sender-profile.service';
import { AuthService } from '../../shared/services/auth.service';

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
    status: 'DRAFT'
  };

  groups: any[] = [];
  senderProfiles: any[] = [];
  error = '';
  success = '';
  loading = false;

  constructor(
    private campaignService: CampaignService,
    private groupService: TargetGroupService,
    private senderProfileService: SenderProfileService,
    public router: Router,  // ✅ CHANGÉ : private → public
    private route: ActivatedRoute,
    public authService: AuthService
  ) {}

  ngOnInit() {
    // Charger les groupes et profils
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

    // Vérifier si on est en mode édition
    const id = this.route.snapshot.params['id'];
    if (id) {
      this.isEditMode = true;
      this.campaignId = id;
      this.loadCampaign(id);
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
          status: data.status || 'DRAFT'
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

    const payload = {
      name: this.form.name,
      senderEmail: this.form.senderEmail,
      senderProfileId: this.form.senderProfileId || null,
      targetGroupId: this.form.targetGroupId,
      scheduledAt: this.form.scheduledAt ? new Date(this.form.scheduledAt).toISOString() : null,
      dryRun: this.form.dryRun,
      dryRunEmail: this.form.dryRunEmail || null,
      throttleSeconds: this.form.throttleSeconds || 5,
      status: this.form.status
    };

    if (this.isEditMode) {
      // ✅ MODE ÉDITION
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
      // ✅ MODE CRÉATION
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