import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { CampaignService } from '../../shared/services/campaign.service';
import { TargetGroupService } from '../../shared/services/target-group.service';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-campaign-list',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent, DatePipe],
  templateUrl: './campaign-list.component.html'
})
export class CampaignListComponent implements OnInit {

  campaigns: any[] = [];
  groups: any[] = [];
  loading = true;
  error = '';

  constructor(
    public authService: AuthService,
    private campaignService: CampaignService,
    private groupService: TargetGroupService
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
      error: () => { this.error = 'Erreur lors du chargement'; this.loading = false; }
    });
  }

  getGroupName(groupId: string): string {
    const group = this.groups.find(g => g.id === groupId);
    return group ? group.name : '';
  }

  // ✅ Correction : utiliser getUserId()
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
        this.error = err.error?.message || 'Erreur lors de l\'autorisation';
      }
    });
  }

  clone(id: string) {
    this.campaignService.clone(id).subscribe({
      next: () => this.load(),
      error: () => { this.error = 'Erreur lors du clonage'; }
    });
  }

  pause(id: string) {
    this.campaignService.pause(id).subscribe({
      next: (updated) => {
        const index = this.campaigns.findIndex(c => c.id === id);
        if (index !== -1) this.campaigns[index] = updated;
      },
      error: () => { this.error = 'Erreur lors de la pause'; }
    });
  }

  resume(id: string) {
    this.campaignService.resume(id).subscribe({
      next: (updated) => {
        const index = this.campaigns.findIndex(c => c.id === id);
        if (index !== -1) this.campaigns[index] = updated;
      },
      error: () => { this.error = 'Erreur lors de la reprise'; }
    });
  }

  delete(id: string) {
    if (!confirm('Supprimer cette campagne ?')) return;
    this.campaignService.delete(id).subscribe({
      next: () => { this.campaigns = this.campaigns.filter(c => c.id !== id); },
      error: () => { this.error = 'Erreur lors de la suppression'; }
    });
  }

  // ✅ NOUVELLE MÉTHODE : Envoyer une campagne (message adapté au Dry Run)
  sendCampaign(id: string) {
    // ✅ Récupérer la campagne pour vérifier le mode Dry Run
    const campaign = this.campaigns.find(c => c.id === id);
    if (!campaign) {
      this.error = 'Campagne non trouvée';
      return;
    }

    // ✅ Message adapté au mode Dry Run
    let confirmMessage = '⚠️ Envoyer cette campagne ';
    if (campaign.dryRun) {
      confirmMessage += `en mode DRY RUN (uniquement à ${campaign.dryRunEmail || 'l\'adresse de test'}) ?`;
    } else {
      confirmMessage += `à toutes les cibles sélectionnées (groupe : ${this.getGroupName(campaign.targetGroupId) || 'Non défini'}) ?`;
    }
    
    if (!confirm(confirmMessage)) return;
    
    this.loading = true;
    this.campaignService.sendCampaign(id).subscribe({
      next: (response) => {
        this.loading = false;
        this.load();
        this.error = '';
      },
      error: (err) => {
        this.loading = false;
        this.error = typeof err.error === 'string' ? err.error : 'Erreur lors de l\'envoi';
      }
    });
  }

  getStatusColor(status: string): string {
    switch(status) {
      case 'AUTHORIZED': return '#4ade80';
      case 'RUNNING': return '#60a5fa';
      case 'PAUSED': return '#f59e0b';
      case 'SCHEDULED': return '#a78bfa';
      case 'COMPLETED': return '#8899aa';
      default: return '#e07b2a';
    }
  }

  getStatusBg(status: string): string {
    switch(status) {
      case 'AUTHORIZED': return 'rgba(34,197,94,0.1)';
      case 'RUNNING': return 'rgba(59,130,246,0.1)';
      case 'PAUSED': return 'rgba(245,158,11,0.1)';
      case 'SCHEDULED': return 'rgba(167,139,250,0.1)';
      case 'COMPLETED': return 'rgba(136,153,170,0.1)';
      default: return 'rgba(224,123,42,0.1)';
    }
  }
}