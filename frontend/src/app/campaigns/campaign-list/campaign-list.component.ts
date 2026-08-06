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
  styles: [`
    .action-btn {
      padding: 6px 8px;
      border-radius: 6px;
      border: 1px solid transparent;
      cursor: pointer;
      font-size: 14px;
      transition: all 0.2s;
      background: transparent;
    }

    .action-btn-edit {
      color: #e07b2a;
      border-color: rgba(224,123,42,0.2);
      background: rgba(224,123,42,0.08);
    }
    .action-btn-edit:hover {
      background: rgba(224,123,42,0.2);
    }

    .action-btn-authorize {
      color: #22c55e;
      border-color: rgba(34,197,94,0.2);
      background: rgba(34,197,94,0.08);
    }
    .action-btn-authorize:hover {
      background: rgba(34,197,94,0.2);
    }

    .action-btn-send {
      color: #3b82f6;
      border-color: rgba(59,130,246,0.2);
      background: rgba(59,130,246,0.08);
    }
    .action-btn-send:hover {
      background: rgba(59,130,246,0.2);
    }

    .action-btn-clone {
      color: #8b5cf6;
      border-color: rgba(139,92,246,0.2);
      background: rgba(139,92,246,0.08);
    }
    .action-btn-clone:hover {
      background: rgba(139,92,246,0.2);
    }

    .action-btn-pause {
      color: #f59e0b;
      border-color: rgba(245,158,11,0.2);
      background: rgba(245,158,11,0.08);
    }
    .action-btn-pause:hover {
      background: rgba(245,158,11,0.2);
    }

    .action-btn-resume {
      color: #3b82f6;
      border-color: rgba(59,130,246,0.2);
      background: rgba(59,130,246,0.08);
    }
    .action-btn-resume:hover {
      background: rgba(59,130,246,0.2);
    }

    .action-btn-delete {
      color: #dc2626;
      border-color: rgba(239,68,68,0.2);
      background: rgba(239,68,68,0.08);
    }
    .action-btn-delete:hover {
      background: rgba(239,68,68,0.2);
    }
  `]
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
      error: () => { this.error = 'Erreur lors du chargement'; this.loading = false; }
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

  sendCampaign(id: string) {
    const campaign = this.campaigns.find(c => c.id === id);
    if (!campaign) {
      this.error = 'Campagne non trouvée';
      return;
    }

    let confirmMessage = '⚠️ Envoyer cette campagne ';
    if (campaign.dryRun) {
      confirmMessage += `en mode DRY RUN (uniquement à ${campaign.dryRunEmail || 'l\'adresse de test'}) ?`;
    } else {
      confirmMessage += `à toutes les cibles sélectionnées (groupe : ${this.getGroupName(campaign.targetGroupId) || 'Non défini'}) ?`;
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
        this.error = typeof err.error === 'string' ? err.error : 'Erreur lors de l\'envoi';
      }
    });
  }

  getStatusColor(status: string): string {
    switch(status) {
      case 'AUTHORIZED': return '#22c55e';
      case 'RUNNING': return '#3b82f6';
      case 'PAUSED': return '#f59e0b';
      case 'SCHEDULED': return '#8b5cf6';
      case 'COMPLETED': return '#8899aa';
      default: return '#f59e0b';
    }
  }

  getStatusBg(status: string): string {
    switch(status) {
      case 'AUTHORIZED': return 'rgba(34,197,94,0.15)';
      case 'RUNNING': return 'rgba(59,130,246,0.15)';
      case 'PAUSED': return 'rgba(245,158,11,0.15)';
      case 'SCHEDULED': return 'rgba(139,92,246,0.15)';
      case 'COMPLETED': return 'rgba(136,153,170,0.15)';
      default: return 'rgba(245,158,11,0.15)';
    }
  }
}