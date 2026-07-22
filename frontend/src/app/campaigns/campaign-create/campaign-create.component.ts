import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { CampaignService } from '../../shared/services/campaign.service';
import { TargetGroupService } from '../../shared/services/target-group.service';
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
    targetGroupId: '',
    scheduledAt: '',
    status: 'DRAFT'
  };

  groups: any[] = [];
  error = '';
  success = '';

  constructor(
    private campaignService: CampaignService,
    private groupService: TargetGroupService,
    private router: Router,
    public authService: AuthService
  ) {}

  ngOnInit() {
    this.groupService.getAll().subscribe({
      next: (data) => {
        this.groups = data;
        if (data.length > 0) this.form.targetGroupId = data[0].id;
      },
      error: () => { this.error = 'Erreur lors du chargement des groupes'; }
    });
  }

  create() {
    if (!this.form.name || !this.form.senderEmail || !this.form.targetGroupId) {
      this.error = 'Le nom, l\'email expéditeur et le groupe sont obligatoires';
      return;
    }

    const payload = {
      ...this.form,
      scheduledAt: this.form.scheduledAt ? new Date(this.form.scheduledAt).toISOString() : null
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