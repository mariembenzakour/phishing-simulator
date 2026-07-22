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
  templateUrl: './target-list.component.html'
})
export class TargetListComponent implements OnInit {

  targets: any[] = [];
  groups: any[] = [];
  selectedGroupId = '';
  loading = true;
  
  uploadMessage = '';
  uploadSuccess = false;
  selectedFile: File | null = null;

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

  delete(id: string) {
    if (!confirm('Supprimer cette cible ?')) return;
    this.targetService.delete(id).subscribe({
      next: () => { 
        this.targets = this.targets.filter(t => t.id !== id); 
      }
    });
  }

  // ✅ Sélection du fichier
  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.uploadCsv();
    }
  }

  // ✅ Upload du CSV avec rechargement automatique
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
        
        // ✅ Recharger la liste des cibles APRÈS l'import
        this.loadTargets(this.selectedGroupId);
        
        // ✅ Réinitialiser le message après 5 secondes
        setTimeout(() => {
          this.uploadMessage = '';
        }, 5000);
      },
      error: (err) => {
        this.uploadMessage = typeof err.error === 'string' ? err.error : 'Erreur lors de l\'import';
        this.uploadSuccess = false;
        // ✅ Réinitialiser après 5 secondes même en cas d'erreur
        setTimeout(() => {
          this.uploadMessage = '';
        }, 5000);
      }
    });
  }
}