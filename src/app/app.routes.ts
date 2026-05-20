import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { guestGuard } from './core/guards/guest-guard';
import { LoginComponent } from './pages/login/login';
import { RegisterComponent } from './pages/register/register';
import { LayoutComponent } from './shared/layout/layout';
import { EventListComponent } from './pages/event-list/event-list';
import { EventDetailComponent } from './pages/event-detail/event-detail';
import { EventCreateComponent } from './pages/event-create/event-create';
import { EventEditComponent } from './pages/event-edit/event-edit';
import { MyEventsComponent } from './pages/my-events/my-events';
import { MyLikesComponent } from './pages/my-likes/my-likes';
import { MyParticipationsComponent } from './pages/my-participations/my-participations';

export const routes: Routes = [
  { path: 'login',    component: LoginComponent,    canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'events',            component: EventListComponent },
      { path: 'events/create',     component: EventCreateComponent },
      { path: 'events/:id/edit',   component: EventEditComponent },
      { path: 'events/:id',        component: EventDetailComponent },
      { path: 'my-events',         component: MyEventsComponent },
      { path: 'my-likes',          component: MyLikesComponent },
      { path: 'my-participations', component: MyParticipationsComponent },
      { path: '', redirectTo: 'events', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'events' }
];