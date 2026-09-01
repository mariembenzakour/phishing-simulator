import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { TargetGroupService } from '../../shared/services/target-group.service';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-group-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './group-list.component.html',
  styleUrl: './group-list.component.scss'
})
export class GroupListComponent implements OnInit {

  groups: any[] = [];
  loading = true;

  // ✅ Variables pour l'édition
  editingGroup: any = null;
  editGroupName: string = '';
  editError = '';
  editSuccess = '';

  constructor(public authService: AuthService, private groupService: TargetGroupService) {}

  ngOnInit() {
    this.loadGroups();
  }

  loadGroups() {
    this.loading = true;
    this.groupService.getAll().subscribe({
      next: (data) => { this.groups = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  // ✅ Ouvrir le modal d'édition
  openEditGroup(group: any) {
    this.editingGroup = group;
    this.editGroupName = group.name;
    this.editError = '';
    this.editSuccess = '';
  }

  // ✅ Fermer le modal
  closeEditGroup() {
    this.editingGroup = null;
    this.editGroupName = '';
    this.editError = '';
    this.editSuccess = '';
  }

  // ✅ Sauvegarder les modifications
  saveEditGroup() {
    if (!this.editGroupName.trim()) {
      this.editError = 'Le nom du groupe est obligatoire';
      return;
    }

    const updatedGroup = {
      name: this.editGroupName.trim(),
      createdBy: this.editingGroup.createdBy
    };

    this.groupService.update(this.editingGroup.id, updatedGroup).subscribe({
      next: () => {
        this.editSuccess = 'Groupe modifié avec succès !';
        setTimeout(() => {
          this.closeEditGroup();
          this.loadGroups();
        }, 1500);
      },
      error: (err) => {
        this.editError = err.error?.message || 'Erreur lors de la modification';
      }
    });
  }

  delete(id: string) {
    if (!confirm('Voulez-vous vraiment supprimer ce groupe ?')) return;
    this.groupService.delete(id).subscribe({
      next: () => { this.groups = this.groups.filter(g => g.id !== id); }
    });
  }
}