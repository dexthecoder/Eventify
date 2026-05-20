import { Component, inject, signal, OnInit, HostListener } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { EventService } from '../../core/services/event';
import { EventResponse, ECategory, EStatus } from '../../models/event.model';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-my-events',
  standalone: true,
  imports: [RouterModule, DatePipe],
  templateUrl: './my-events.html',
  styleUrl: './my-events.scss'
})
export class MyEventsComponent implements OnInit {
  private eventService = inject(EventService);
  private router = inject(Router);

  events = signal<EventResponse[]>([]);
  loading = signal(true);
  totalPages = signal(0);
  currentPage = signal(0);

  search = signal('');
  selectedCategory = signal<ECategory | undefined>(undefined);
  selectedStatus = signal<EStatus | undefined>(undefined);
  selectedSortBy = signal('creationDate');
  selectedDirection = signal('DESC');
  sortDropdownOpen = signal(false);
  confirmDeleteId = signal<number | null>(null);

  categories: { label: string; value: ECategory | undefined }[] = [
    { label: 'Tümü',      value: undefined },
    { label: 'Müzik',     value: 'MUSIC' },
    { label: 'Spor',      value: 'SPORT' },
    { label: 'Atölye',    value: 'WORKSHOP' },
    { label: 'Tiyatro',   value: 'THEATER' },
    { label: 'Teknoloji', value: 'TECH' },
    { label: 'Diğer',     value: 'OTHER' },
  ];

  statusOptions: { label: string; value: EStatus | undefined; icon: string }[] = [
    { label: 'Tümü',              value: undefined,      icon: 'bi-grid' },
    { label: 'Yayında',           value: 'PUBLISHED',    icon: 'bi-broadcast' },
    { label: 'Yayın Durduruldu',  value: 'UNPUBLISHED',  icon: 'bi-pause-circle' },
    { label: 'Arşivlendi',        value: 'ARCHIVED',     icon: 'bi-archive' },
  ];

  sortOptions: { label: string; sortBy: string; direction: string }[] = [
    { label: 'En Yeni Eklenen', sortBy: 'creationDate',  direction: 'DESC' },
    { label: 'En Eski Eklenen', sortBy: 'creationDate',  direction: 'ASC' },
    { label: 'En Yakın Tarih',  sortBy: 'executionDate', direction: 'ASC' },
    { label: 'En Uzak Tarih',   sortBy: 'executionDate', direction: 'DESC' },
  ];

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.sort-dropdown-wrap')) {
      this.sortDropdownOpen.set(false);
    }
  }

  ngOnInit() { this.loadEvents(); }

  loadEvents() {
    this.loading.set(true);
    this.eventService.getMyEvents(
      this.currentPage(), 12,
      this.selectedSortBy(),
      this.selectedDirection(),
      this.search(),
      this.selectedCategory()
    ).subscribe({
      next: (page) => {
        this.events.set(page.content);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onSearch(value: string) {
    this.search.set(value);
    this.currentPage.set(0);
    this.loadEvents();
  }

  onCategoryChange(value: ECategory | undefined) {
    this.selectedCategory.set(value);
    this.currentPage.set(0);
    this.loadEvents();
  }

  onStatusChange(value: EStatus | undefined) {
    this.selectedStatus.set(value);
    this.currentPage.set(0);
    this.loadEvents();
  }

  onSortChange(sortBy: string, direction: string) {
    this.selectedSortBy.set(sortBy);
    this.selectedDirection.set(direction);
    this.currentPage.set(0);
    this.sortDropdownOpen.set(false);
    this.loadEvents();
  }

  isActiveSortOption(sortBy: string, direction: string): boolean {
    return this.selectedSortBy() === sortBy && this.selectedDirection() === direction;
  }

  activeSortLabel(): string {
    return this.sortOptions.find(o =>
      o.sortBy === this.selectedSortBy() && o.direction === this.selectedDirection()
    )?.label ?? 'Sırala';
  }

  goToPage(page: number) {
    this.currentPage.set(page);
    this.loadEvents();
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  get filteredEvents(): EventResponse[] {
    const status = this.selectedStatus();
    if (!status) return this.events();
    return this.events().filter(e => e.status === status);
  }

  getStatusBadge(status: EStatus): { label: string; cls: string } {
    switch (status) {
      case 'PUBLISHED':   return { label: 'Yayında',          cls: 'badge-published' };
      case 'UNPUBLISHED': return { label: 'Yayın Durduruldu', cls: 'badge-unpublished' };
      case 'ARCHIVED':    return { label: 'Arşivlendi',       cls: 'badge-archived' };
    }
  }

  get imageUrl() {
    return (event: EventResponse) => {
      const path = event.imagePath;
      if (!path) return 'images/default_other.jpg';
      if (path.startsWith('default_')) return `images/${path}`;
      return `http://localhost:8050/uploads/event-images/${path}`;
    };
  }

  changeStatus(event: EventResponse, newStatus: EStatus) {
    this.eventService.update(event.id, { status: newStatus }).subscribe({
      next: () => {
        this.events.set(
          this.events().map(e => e.id === event.id ? { ...e, status: newStatus } : e)
        );
      },
      error: () => {}
    });
  }

  confirmDelete(eventId: number) {
    this.confirmDeleteId.set(eventId);
  }

  cancelDelete() {
    this.confirmDeleteId.set(null);
  }

  deleteEvent(eventId: number) {
    this.eventService.delete(eventId).subscribe({
      next: () => {
        this.events.set(this.events().filter(e => e.id !== eventId));
        this.confirmDeleteId.set(null);
      },
      error: () => {}
    });
  }

  goToEdit(eventId: number) {
    this.router.navigate(['/events', eventId, 'edit']);
  }

  goToDetail(eventId: number) {
    this.router.navigate(['/events', eventId]);
  }
}