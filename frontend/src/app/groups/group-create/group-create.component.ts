import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { TargetGroupService } from '../../shared/services/target-group.service';

@Component({
  selector: 'app-group-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './group-create.component.html'
})
export class GroupCreateComponent {

  name = '';
  error = '';
  success = '';

  constructor(private groupService: TargetGroupService, private router: Router) {}

  create() {
    if (!this.name.trim()) { this.error = 'Le nom est obligatoire'; return; }
    this.groupService.create({ name: this.name }).subscribe({
      next: () => {
        this.success = 'Groupe créé !';
        setTimeout(() => this.router.navigate(['/groups']), 1500);
      },
      error: () => { this.error = 'Erreur lors de la création'; }
    });
  }
}