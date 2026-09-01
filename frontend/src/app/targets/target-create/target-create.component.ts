import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { TargetService } from '../../shared/services/target.service';
import { TargetGroupService } from '../../shared/services/target-group.service';

@Component({
  selector: 'app-target-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './target-create.component.html',
  styleUrl: './target-create.component.scss'
})
export class TargetCreateComponent implements OnInit {

  form = { firstName: '', lastName: '', email: '', groupId: '' };
  groups: any[] = [];
  error = '';
  success = '';

  constructor(
    private targetService: TargetService,
    private groupService: TargetGroupService,
    private router: Router
  ) {}

  ngOnInit() {
    this.groupService.getAll().subscribe({
      next: (data) => {
        this.groups = data;
        if (data.length > 0) this.form.groupId = data[0].id;
      }
    });
  }

  create() {
    if (!this.form.firstName || !this.form.lastName || !this.form.email || !this.form.groupId) {
      this.error = 'Tous les champs sont obligatoires';
      return;
    }
    this.targetService.create(this.form).subscribe({
      next: () => {
        this.success = 'Cible ajoutée !';
        setTimeout(() => this.router.navigate(['/targets']), 1500);
      },
      error: () => { this.error = 'Erreur lors de la création'; }
    });
  }
}