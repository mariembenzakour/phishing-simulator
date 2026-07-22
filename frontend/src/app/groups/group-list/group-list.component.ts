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
  templateUrl: './group-list.component.html'
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