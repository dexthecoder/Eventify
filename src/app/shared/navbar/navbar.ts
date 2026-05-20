import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/services/auth';

@Component({
  selector: 'app-navbar',
  imports: [RouterModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(window:scroll)': 'onScroll()',
    '(document:click)': 'onDocumentClick($event)',
  },
})
export class NavbarComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  currentUser = toSignal(this.authService.currentUser$, { initialValue: this.authService.currentUser });
  isScrolled = signal(false);
  isProfileOpen = signal(false);
  isMobileMenuOpen = signal(false);

  navLinks = [
    { label: 'Etkinlikler',   path: '/events' },
    { label: 'Katıldıklarım', path: '/my-participations' },
    { label: 'Beğendiklerim', path: '/my-likes' },
  ];

  onScroll() {
    this.isScrolled.set(window.scrollY > 20);
  }

  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.profile-menu')) {
      this.isProfileOpen.set(false);
    }
  }

  toggleProfile() {
    this.isProfileOpen.set(!this.isProfileOpen());
  }

  toggleMobileMenu() {
    this.isMobileMenuOpen.set(!this.isMobileMenuOpen());
  }

  logout() {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login']),
    });
  }
}
