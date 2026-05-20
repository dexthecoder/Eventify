import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { EventService } from '../../core/services/event';
import { ECategory } from '../../models/event.model';

@Component({
  selector: 'app-event-create',
  standalone: true,
  imports: [ReactiveFormsModule, RouterModule],
  templateUrl: './event-create.html',
  styleUrl: './event-create.scss'
})
export class EventCreateComponent {
  private fb = inject(FormBuilder);
  private eventService = inject(EventService);
  private router = inject(Router);

  loading = signal(false);
  errorMsg = signal('');
  imagePreview = signal<string | null>(null);
  selectedFile: File | null = null;

  categories: { label: string; value: ECategory }[] = [
    { label: 'Müzik',     value: 'MUSIC' },
    { label: 'Spor',      value: 'SPORT' },
    { label: 'Atölye',    value: 'WORKSHOP' },
    { label: 'Tiyatro',   value: 'THEATER' },
    { label: 'Teknoloji', value: 'TECH' },
    { label: 'Diğer',     value: 'OTHER' },
  ];

  form = this.fb.group({
    title:         ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
    description:   ['', [Validators.required, Validators.maxLength(2000)]],
    
    executionDate: ['', [
    Validators.required,
    (control: any) => {
      if (!control.value) return null;
      const selectedStr = control.value;
      const now = new Date();
      const nowStr = now.getFullYear() + '-' +
        String(now.getMonth() + 1).padStart(2, '0') + '-' +
        String(now.getDate()).padStart(2, '0') + 'T' +
        String(now.getHours()).padStart(2, '0') + ':' +
        String(now.getMinutes()).padStart(2, '0');
      return selectedStr <= nowStr ? { futureDate: true } : null;
      }
    ]],

    location:      ['', [Validators.required, Validators.maxLength(250)]],
    category:      ['' as ECategory, [Validators.required]],
  });

  get f() { return this.form.controls; }

  get minDate(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    // Boyut kontrolü (2MB)
    if (file.size > 2 * 1024 * 1024) {
      this.errorMsg.set('Dosya boyutu 2MB\'ı geçemez.');
      return;
    }

    // Tip kontrolü
    if (!['image/jpeg', 'image/jpg', 'image/png'].includes(file.type)) {
      this.errorMsg.set('Sadece JPG, JPEG ve PNG dosyaları yüklenebilir.');
      return;
    }

    this.selectedFile = file;
    this.errorMsg.set('');

    // Önizleme
    const reader = new FileReader();
    reader.onload = () => this.imagePreview.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  removeImage() {
    this.selectedFile = null;
    this.imagePreview.set(null);
  }

  onSubmit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.errorMsg.set('');

    const val = this.form.value;

    // LocalDateTime formatına çevir
    const executionDate = val.executionDate!;

    this.eventService.create({
      title:         val.title!,
      description:   val.description!,
      executionDate: executionDate,
      location:      val.location!,
      category:      val.category! as ECategory,
    }, this.selectedFile ?? undefined).subscribe({
      next: (event) => this.router.navigate(['/events', event.id]),
      error: (err: any) => {

        if (Array.isArray(err.error)) {
          const messages = err.error.map((e: any) => e.message).join(' | ');
          this.errorMsg.set(messages);
        } else {
          this.errorMsg.set(err.error?.message ?? 'Etkinlik oluşturulamadı.');
        }
        this.loading.set(false);
        
      }
    });
  }
}