import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  form = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(5)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  loading = signal(false);
  errorMsg = signal('');
  showPassword = signal(false);

  get username() { return this.form.get('username')!; }
  get password() { return this.form.get('password')!; }

  onSubmit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.errorMsg.set('');

    this.authService.login(this.form.value as { username: string; password: string }).subscribe({
      next: () => this.router.navigate(['/events']),
      error: (err: { error?: { message?: string } }) => {
        this.errorMsg.set(err.error?.message ?? 'Giriş başarısız.');
        this.loading.set(false);
      },
    });
  }
}
