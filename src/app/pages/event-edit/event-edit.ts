import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { EventService } from '../../core/services/event';
import { ECategory, EStatus, EventResponse } from '../../models/event.model';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-event-edit',
  standalone: true,
  imports: [ReactiveFormsModule, RouterModule, DatePipe],
  templateUrl: './event-edit.html',
  styleUrl: './event-edit.scss'
})
export class EventEditComponent implements OnInit {
  private fb = inject(FormBuilder);
  private eventService = inject(EventService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  loading = signal(false);
  pageLoading = signal(true);
  errorMsg = signal('');
  imagePreview = signal<string | null>(null);
  selectedFile: File | null = null;
  currentEvent = signal<EventResponse | null>(null);

  categories: { label: string; value: ECategory }[] = [
    { label: 'Müzik',     value: 'MUSIC' },
    { label: 'Spor',      value: 'SPORT' },
    { label: 'Atölye',    value: 'WORKSHOP' },
    { label: 'Tiyatro',   value: 'THEATER' },
    { label: 'Teknoloji', value: 'TECH' },
    { label: 'Diğer',     value: 'OTHER' },
  ];

  statusOptions: { label: string; value: EStatus; icon: string }[] = [
    { label: 'Yayında',          value: 'PUBLISHED',   icon: 'bi-broadcast' },
    { label: 'Yayın Durduruldu', value: 'UNPUBLISHED', icon: 'bi-pause-circle' },
    { label: 'Arşivlendi',       value: 'ARCHIVED',    icon: 'bi-archive' },
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
    status:        ['' as EStatus],
  });

  get f() { return this.form.controls; }

  get minDate(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  }

  get isArchived(): boolean {
    return this.currentEvent()?.status === 'ARCHIVED';
  }

  get currentImageUrl(): string {
    const path = this.currentEvent()?.imagePath;
    if (!path) return 'images/default_other.jpg';
    if (path.startsWith('default_')) return `images/${path}`;
    return `http://localhost:8050/uploads/event-images/${path}`;
  }

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.eventService.getById(id).subscribe({
      next: (event) => {
        this.currentEvent.set(event);

        // Tarihi datetime-local formatına çevir
        const date = new Date(event.executionDate);
        date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
        const formattedDate = date.toISOString().slice(0, 16);

        this.form.patchValue({
          title:         event.title,
          description:   event.description,
          executionDate: formattedDate,
          location:      event.location,
          category:      event.category,
          status:        event.status,
        });

        // Arşivlenmişse formu devre dışı bırak
        if (event.status === 'ARCHIVED') {
          this.form.disable();
        }

        this.pageLoading.set(false);
      },
      error: () => this.router.navigate(['/my-events'])
    });
  }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (file.size > 2 * 1024 * 1024) {
      this.errorMsg.set('Dosya boyutu 2MB\'ı geçemez.');
      return;
    }

    if (!['image/jpeg', 'image/jpg', 'image/png'].includes(file.type)) {
      this.errorMsg.set('Sadece JPG, JPEG ve PNG dosyaları yüklenebilir.');
      return;
    }

    this.selectedFile = file;
    this.errorMsg.set('');

    const reader = new FileReader();
    reader.onload = () => this.imagePreview.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  removeImage() {
    this.selectedFile = null;
    this.imagePreview.set(null);
  }

  onSubmit() {
    if (this.form.invalid || this.isArchived) return;
    this.loading.set(true);
    this.errorMsg.set('');

    const val = this.form.value;
    const executionDate = val.executionDate!;

    this.eventService.update(
      this.currentEvent()!.id,
      {
        title:         val.title ?? undefined,
        description:   val.description ?? undefined,
        executionDate: executionDate,
        location:      val.location ?? undefined,
        category:      val.category as ECategory,
        status:        val.status as EStatus,
      },
      this.selectedFile ?? undefined
    ).subscribe({
      next: () => this.router.navigate(['/my-events']),
      error: (err: any) => {
        
      if (Array.isArray(err.error)) {
        const messages = err.error.map((e: any) => e.message).join(' | ');
        this.errorMsg.set(messages);
      } else {
        this.errorMsg.set(err.error?.message ?? 'Güncellenemedi.');
      }
      this.loading.set(false);
        
      }
    });
  }
}