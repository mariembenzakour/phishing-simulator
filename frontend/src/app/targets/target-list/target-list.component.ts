import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { TargetService } from '../../shared/services/target.service';
import { TargetGroupService } from '../../shared/services/target-group.service';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-target-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './target-list.component.html',
  styleUrl: './target-list.component.scss'
})
export class TargetListComponent implements OnInit {

  targets: any[] = [];
  groups: any[] = [];
  selectedGroupId = '';
  loading = true;
  
  uploadMessage = '';
  uploadSuccess = false;
  selectedFile: File | null = null;

  // ✅ Variables pour l'édition
  editingTarget: any = null;
  editTarget: any = {};
  editError = '';
  editSuccess = '';

  constructor(
    public authService: AuthService,
    private targetService: TargetService,
    private groupService: TargetGroupService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.groupService.getAll().subscribe({
      next: (data) => {
        this.groups = data;
        const groupId = this.route.snapshot.queryParams['groupId'];
        if (groupId) {
          this.selectedGroupId = groupId;
          this.loadTargets(groupId);
        } else if (data.length > 0) {
          this.selectedGroupId = data[0].id;
          this.loadTargets(data[0].id);
        } else {
          this.loading = false;
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadTargets(groupId: string) {
    this.loading = true;
    this.targetService.getByGroup(groupId).subscribe({
      next: (data) => { 
        this.targets = data; 
        this.loading = false; 
      },
      error: () => { 
        this.loading = false; 
      }
    });
  }

  onGroupChange(groupId: string) {
    this.selectedGroupId = groupId;
    this.loadTargets(groupId);
  }

  // ✅ Ouvrir le modal d'édition
  openEditTarget(target: any) {
    this.editingTarget = target;
    this.editTarget = {
      id: target.id,
      firstName: target.firstName,
      lastName: target.lastName,
      email: target.email,
      groupId: target.groupId
    };
    this.editError = '';
    this.editSuccess = '';
  }

  // ✅ Fermer le modal
  closeEditTarget() {
    this.editingTarget = null;
    this.editTarget = {};
    this.editError = '';
    this.editSuccess = '';
  }

  // ✅ Sauvegarder les modifications
  saveEditTarget() {
    if (!this.editTarget.firstName.trim() || !this.editTarget.lastName.trim() || !this.editTarget.email.trim()) {
      this.editError = 'Tous les champs sont obligatoires';
      return;
    }

    if (!this.editTarget.groupId) {
      this.editError = 'Veuillez sélectionner un groupe';
      return;
    }

    this.targetService.update(this.editingTarget.id, this.editTarget).subscribe({
      next: () => {
        this.editSuccess = 'Cible modifiée avec succès !';
        setTimeout(() => {
          this.closeEditTarget();
          this.loadTargets(this.selectedGroupId);
        }, 1500);
      },
      error: (err) => {
        this.editError = err.error?.message || 'Erreur lors de la modification';
      }
    });
  }

  delete(id: string) {
    if (!confirm('Supprimer cette cible ?')) return;
    this.targetService.delete(id).subscribe({
      next: () => { 
        this.targets = this.targets.filter(t => t.id !== id); 
      }
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.uploadCsv();
    }
  }

  uploadCsv() {
    if (!this.selectedFile) {
      this.uploadMessage = 'Veuillez sélectionner un fichier CSV';
      this.uploadSuccess = false;
      return;
    }

    if (!this.selectedGroupId) {
      this.uploadMessage = 'Veuillez sélectionner un groupe';
      this.uploadSuccess = false;
      return;
    }

    if (!this.selectedFile.name.endsWith('.csv')) {
      this.uploadMessage = 'Le fichier doit être au format CSV';
      this.uploadSuccess = false;
      return;
    }

    this.targetService.uploadCsv(this.selectedFile, this.selectedGroupId).subscribe({
      next: (response) => {
        this.uploadMessage = response;
        this.uploadSuccess = true;
        this.selectedFile = null;
        this.loadTargets(this.selectedGroupId);
        setTimeout(() => {
          this.uploadMessage = '';
        }, 5000);
      },
      error: (err) => {
        this.uploadMessage = typeof err.error === 'string' ? err.error : 'Erreur lors de l\'import';
        this.uploadSuccess = false;
        setTimeout(() => {
          this.uploadMessage = '';
        }, 5000);
      }
    });
  }
}