import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { TargetGroupService } from '../../shared/services/target-group.service';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-group-list',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent],
  templateUrl: './group-list.component.html',
  styles: [`
    .group-card {
      background: white;
      border-radius: 12px;
      padding: 24px;
      border: 1px solid #e2e8f0;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
      transition: transform 0.2s, box-shadow 0.2s;
    }
    .group-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0,0,0,0.1);
    }
  `]
})
export class GroupListComponent implements OnInit {

  groups: any[] = [];
  loading = true;

  constructor(public authService: AuthService, private groupService: TargetGroupService) {}

  ngOnInit() {
    this.groupService.getAll().subscribe({
      next: (data) => { this.groups = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  delete(id: string) {
    if (!confirm('Supprimer ce groupe ?')) return;
    this.groupService.delete(id).subscribe({
      next: () => { this.groups = this.groups.filter(g => g.id !== id); }
    });
  }
}