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
import { OperatorCreateComponent } from './operators/operator-create/operator-create.component';
import { authGuard } from './shared/guards/auth.guard';
import { GlobalDashboardComponent } from './dashboard/global-dashboard/global-dashboard.component';
import { CampaignDetailComponent } from './dashboard/campaign-detail/campaign-detail.component';
import { UserReportComponent } from './dashboard/user-report/user-report.component';
import { TrendsComponent } from './dashboard/trends/trends.component';

// ✅ IMPORT DES COMPOSANTS IA (Week 5)
import { AiGenerationComponent } from './ai/ai-generation.component';
import { AiDraftsComponent } from './ai/ai-drafts.component';
import { AuditLogComponent } from './audit/audit-log.component';

export const routes: Routes = [
  // ── AUTHENTIFICATION ──────────────────────────
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'setup-mfa', component: SetupMfaComponent },
  { path: 'mfa', component: MfaComponent },
  { path: 'unauthorized', component: UnauthorizedComponent },

  // ── CAMPAGNES ──────────────────────────────────
  { 
    path: 'campaigns', 
    component: CampaignListComponent, 
    canActivate: [authGuard], 
    data: { roles: ['ADMIN', 'OPERATOR', 'VIEWER', 'SUPER_ADMIN'] } 
  },
  { 
    path: 'campaigns/create', 
    component: CampaignCreateComponent, 
    canActivate: [authGuard], 
    data: { roles: ['ADMIN', 'OPERATOR', 'SUPER_ADMIN'] } 
  },
  { 
    path: 'campaigns/edit/:id', 
    component: CampaignCreateComponent,
    canActivate: [authGuard], 
    data: { roles: ['ADMIN', 'OPERATOR', 'SUPER_ADMIN'] } 
  },

  // ── GROUPES ──────────────────────────────────
  { 
    path: 'groups', 
    component: GroupListComponent, 
    canActivate: [authGuard], 
    data: { roles: ['ADMIN', 'OPERATOR', 'VIEWER', 'SUPER_ADMIN'] } 
  },
  { 
    path: 'groups/create', 
    component: GroupCreateComponent, 
    canActivate: [authGuard], 
    data: { roles: ['ADMIN', 'OPERATOR', 'SUPER_ADMIN'] } 
  },

  // ── CIBLES ───────────────────────────────────
  { 
    path: 'targets', 
    component: TargetListComponent, 
    canActivate: [authGuard], 
    data: { roles: ['ADMIN', 'OPERATOR', 'VIEWER', 'SUPER_ADMIN'] } 
  },
  { 
    path: 'targets/create', 
    component: TargetCreateComponent, 
    canActivate: [authGuard], 
    data: { roles: ['ADMIN', 'OPERATOR', 'SUPER_ADMIN'] } 
  },

  // ── OPÉRATEURS ──────────────────────────────
  { 
    path: 'operators/create', 
    component: OperatorCreateComponent, 
    canActivate: [authGuard], 
    data: { roles: ['ADMIN', 'SUPER_ADMIN'] } 
  },

  // ── DASHBOARD ────────────────────────────────
  { 
    path: 'dashboard', 
    component: GlobalDashboardComponent, 
    canActivate: [authGuard], 
    data: { roles: ['SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER'] } 
  },
  { 
    path: 'dashboard/campaign/:id', 
    component: CampaignDetailComponent, 
    canActivate: [authGuard], 
    data: { roles: ['SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER'] } 
  },
  { 
    path: 'dashboard/users', 
    component: UserReportComponent, 
    canActivate: [authGuard], 
    data: { roles: ['SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER'] } 
  },
  { 
    path: 'dashboard/trends', 
    component: TrendsComponent, 
    canActivate: [authGuard], 
    data: { roles: ['SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER'] } 
  },

  // ── IA (Week 5) ──────────────────────────────
  { 
    path: 'ai/generate', 
    component: AiGenerationComponent, 
    canActivate: [authGuard], 
    data: { roles: ['SUPER_ADMIN', 'ADMIN', 'OPERATOR'] } 
  },
  { 
    path: 'ai/drafts', 
    component: AiDraftsComponent, 
    canActivate: [authGuard], 
    data: { roles: ['SUPER_ADMIN', 'ADMIN', 'OPERATOR'] } 
  },

  {
  path: 'audit',
  component: AuditLogComponent,
  canActivate: [authGuard],
  data: { roles: ['SUPER_ADMIN', 'ADMIN'] }
},

  // ── REDIRECTION 404 ──────────────────────────
  { path: '**', redirectTo: 'login' }
];