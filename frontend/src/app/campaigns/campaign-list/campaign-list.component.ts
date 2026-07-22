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

  authorize(id: string) {
    const operatorId = this.authService.getUserInfo()?.id;
    this.campaignService.authorize(id, operatorId).subscribe({
      next: (updated) => {
        const index = this.campaigns.findIndex(c => c.id === id);
        if (index !== -1) this.campaigns[index] = updated;
      }
    });
  }

  clone(id: string) {
    this.campaignService.clone(id).subscribe({
      next: () => this.load()
    });
  }

  pause(id: string) {
    this.campaignService.pause(id).subscribe({
      next: (updated) => {
        const index = this.campaigns.findIndex(c => c.id === id);
        if (index !== -1) this.campaigns[index] = updated;
      }
    });
  }

  resume(id: string) {
    this.campaignService.resume(id).subscribe({
      next: (updated) => {
        const index = this.campaigns.findIndex(c => c.id === id);
        if (index !== -1) this.campaigns[index] = updated;
      }
    });
  }

  delete(id: string) {
    if (!confirm('Supprimer cette campagne ?')) return;
    this.campaignService.delete(id).subscribe({
      next: () => { this.campaigns = this.campaigns.filter(c => c.id !== id); }
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