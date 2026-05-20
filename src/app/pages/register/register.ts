import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth';
import { UserRegisterRequest } from '../../models/user.model';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrl: './register.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  form = this.fb.group({
    name:     ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    surname:  ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email:    ['', [
      Validators.required,
      Validators.email,
      Validators.pattern('^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,6}$')
    ]],
    phone:    ['', [Validators.required, Validators.pattern('^(05)[0-9]{9}$')]],
    password: ['', [
      Validators.required,
      Validators.minLength(6),
      Validators.maxLength(30),
      Validators.pattern('^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!.\\-_])(?=\\S+$).*$'),
    ]],
  });

  loading = signal(false);
  errorMsg = signal('');
  showPassword = signal(false);

  get f() { return this.form.controls; }

  onSubmit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.errorMsg.set('');

    this.authService.register(this.form.value as UserRegisterRequest).subscribe({
      next: () => this.router.navigate(['/login']),
      error: (err: any) => {
        if (Array.isArray(err.error)) {
          const messages = err.error.map((e: any) => e.message).join(' | ');
          this.errorMsg.set(messages);
        } else {
          this.errorMsg.set(err.error?.message ?? 'Kayıt başarısız.');
        }
        this.loading.set(false);
      },
    });
  }
}