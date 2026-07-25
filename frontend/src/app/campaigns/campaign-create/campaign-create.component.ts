import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
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

  constructor(
    private campaignService: CampaignService,
    private groupService: TargetGroupService,
    private senderProfileService: SenderProfileService,
    private router: Router,
    public authService: AuthService
  ) {}

  ngOnInit() {
    // Charger les groupes
    this.groupService.getAll().subscribe({
      next: (data) => {
        this.groups = data;
        if (data.length > 0) this.form.targetGroupId = data[0].id;
      },
      error: () => { this.error = 'Erreur lors du chargement des groupes'; }
    });

    // ✅ Charger les profils expéditeurs
    this.senderProfileService.getAll().subscribe({
      next: (data) => {
        this.senderProfiles = data;
      },
      error: () => { console.error('Erreur chargement profils'); }
    });
  }

  create() {
    if (!this.form.name || !this.form.senderEmail || !this.form.targetGroupId) {
      this.error = 'Le nom, l\'email expéditeur et le groupe sont obligatoires';
      return;
    }

    // ✅ Vérification Dry Run
    if (this.form.dryRun && !this.form.dryRunEmail) {
      this.error = 'Veuillez renseigner un email de test pour le mode Dry Run';
      return;
    }

    // ✅ Construire le payload
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

    this.campaignService.create(payload).subscribe({
      next: () => {
        this.success = 'Campagne créée avec succès !';
        setTimeout(() => this.router.navigate(['/campaigns']), 1500);
      },
      error: () => { this.error = 'Erreur lors de la création'; }
    });
  }
}