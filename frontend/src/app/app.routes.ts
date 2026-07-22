import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { MfaComponent } from './auth/mfa/mfa.component';
import { RegisterComponent } from './auth/register/register.component';
import { SetupMfaComponent } from './auth/setup-mfa/setup-mfa.component';
import { CampaignListComponent } from './campaigns/campaign-list/campaign-list.component';
import { CampaignCreateComponent } from './campaigns/campaign-create/campaign-create.component';
import { GroupListComponent } from './groups/group-list/group-list.component';
import { GroupCreateComponent } from './groups/group-create/group-create.component';
import { TargetListComponent } from './targets/target-list/target-list.component';
import { TargetCreateComponent } from './targets/target-create/target-create.component';
import { UnauthorizedComponent } from './shared/unauthorized/unauthorized.component';
import { authGuard } from './shared/guards/auth.guard';

// ✅ IMPORT DU NOUVEAU COMPOSANT
import { OperatorCreateComponent } from './operators/operator-create/operator-create.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'setup-mfa', component: SetupMfaComponent },
  { path: 'mfa', component: MfaComponent },
  { path: 'unauthorized', component: UnauthorizedComponent },
  { path: 'campaigns', component: CampaignListComponent, canActivate: [authGuard], data: { roles: ['ADMIN', 'OPERATOR', 'VIEWER'] } },
  { path: 'campaigns/create', component: CampaignCreateComponent, canActivate: [authGuard], data: { roles: ['ADMIN', 'OPERATOR'] } },
  { path: 'groups', component: GroupListComponent, canActivate: [authGuard], data: { roles: ['ADMIN', 'OPERATOR', 'VIEWER'] } },
  { path: 'groups/create', component: GroupCreateComponent, canActivate: [authGuard], data: { roles: ['ADMIN', 'OPERATOR'] } },
  { path: 'targets', component: TargetListComponent, canActivate: [authGuard], data: { roles: ['ADMIN', 'OPERATOR', 'VIEWER'] } },
  { path: 'targets/create', component: TargetCreateComponent, canActivate: [authGuard], data: { roles: ['ADMIN', 'OPERATOR'] } },
  
  // ✅ NOUVELLE ROUTE : Création d'opérateur (ADMIN uniquement)
  { path: 'operators/create', component: OperatorCreateComponent, canActivate: [authGuard], data: { roles: ['ADMIN'] } },
  // ✅ AJOUTER SUPER_ADMIN DANS LES RÔLES AUTORISÉS
  { path: 'operators/create', component: OperatorCreateComponent, canActivate: [authGuard], data: { roles: ['ADMIN', 'SUPER_ADMIN'] } },
  { path: '**', redirectTo: 'login' }
];