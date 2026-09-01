import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { CampaignService } from '../../shared/services/campaign.service';
import { TargetGroupService } from '../../shared/services/target-group.service';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-campaign-list',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent, DatePipe],
  templateUrl: './campaign-list.component.html',
  styleUrl: './campaign-list.component.scss'
})
export class CampaignListComponent implements OnInit {

  campaigns: any[] = [];
  groups: any[] = [];
  loading = true;
  error = '';

  constructor(
    public authService: AuthService,
    private campaignService: CampaignService,
    private groupService: TargetGroupService,
    private router: Router
  ) {}

  ngOnInit() {
    this.load();
    this.groupService.getAll().subscribe({
      next: (data) => { this.groups = data; }
    });
  }

  load() {
    this.loading = true;
    this.campaignService.getAll().subscribe({
      next: (data) => { this.campaigns = data; this.loading = false; },
      error: () => { this.error = 'Erreur lors du chargement des campagnes'; this.loading = false; }
    });
  }

  getGroupName(groupId: string): string {
    const group = this.groups.find(g => g.id === groupId);
    return group ? group.name : '';
  }

  editCampaign(id: string) {
    this.router.navigate(['/campaigns/edit', id]);
  }

  authorize(id: string) {
    const operatorId = this.authService.getUserId();
    if (!operatorId) {
      this.error = 'Utilisateur non identifié';
      return;
    }
    this.campaignService.authorize(id, operatorId).subscribe({
      next: (updated) => {
        const index = this.campaigns.findIndex(c => c.id === id);
        if (index !== -1) this.campaigns[index] = updated;
        this.error = '';
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors de l\'autorisation de la campagne';
      }
    });
  }

  clone(id: string) {
    this.campaignService.clone(id).subscribe({
      next: () => this.load(),
      error: () => { this.error = 'Erreur lors du clonage de la campagne'; }
    });
  }

  pause(id: string) {
    this.campaignService.pause(id).subscribe({
      next: (updated) => {
        const index = this.campaigns.findIndex(c => c.id === id);
        if (index !== -1) this.campaigns[index] = updated;
      },
      error: () => { this.error = 'Erreur lors de la mise en pause de la campagne'; }
    });
  }

  resume(id: string) {
    this.campaignService.resume(id).subscribe({
      next: (updated) => {
        const index = this.campaigns.findIndex(c => c.id === id);
        if (index !== -1) this.campaigns[index] = updated;
      },
      error: () => { this.error = 'Erreur lors de la reprise de la campagne'; }
    });
  }

  delete(id: string) {
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette campagne ?')) return;
    this.campaignService.delete(id).subscribe({
      next: () => { this.campaigns = this.campaigns.filter(c => c.id !== id); },
      error: () => { this.error = 'Erreur lors de la suppression de la campagne'; }
    });
  }

  sendCampaign(id: string) {
    const campaign = this.campaigns.find(c => c.id === id);
    if (!campaign) {
      this.error = 'Campagne non trouvée';
      return;
    }

    let confirmMessage = 'Confirmer l\'envoi immédiat de cette campagne ';
    if (campaign.dryRun) {
      confirmMessage += `en mode DRY RUN (uniquement à ${campaign.dryRunEmail || 'l\'adresse de test'}) ?`;
    } else {
      confirmMessage += `à l'ensemble des cibles du groupe "${this.getGroupName(campaign.targetGroupId) || 'sélectionné'}" ?`;
    }
    
    if (!confirm(confirmMessage)) return;
    
    this.loading = true;
    this.campaignService.sendCampaign(id).subscribe({
      next: () => {
        this.loading = false;
        this.load();
        this.error = '';
      },
      error: (err) => {
        this.loading = false;
        this.error = typeof err.error === 'string' ? err.error : 'Erreur lors du lancement de la campagne';
      }
    });
  }
}