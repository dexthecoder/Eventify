import { Component, inject, signal, OnInit, HostListener } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { ParticipationService } from '../../core/services/participation';
import { EventResponse, ECategory } from '../../models/event.model';

@Component({
  selector: 'app-my-participations',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './my-participations.html',
  styleUrl: './my-participations.scss'
})
export class MyParticipationsComponent implements OnInit {
  private participationService = inject(ParticipationService);
  private router = inject(Router);

  events = signal<EventResponse[]>([]);
  loading = signal(true);
  totalPages = signal(0);
  currentPage = signal(0);

  search = signal('');
  selectedCategory = signal<ECategory | undefined>(undefined);
  selectedStatus = signal<string | undefined>(undefined);
  selectedSortBy = signal('executionDate');
  selectedDirection = signal('DESC');
  sortDropdownOpen = signal(false);

  categories: { label: string; value: ECategory | undefined }[] = [
    { label: 'Tümü',      value: undefined },
    { label: 'Müzik',     value: 'MUSIC' },
    { label: 'Spor',      value: 'SPORT' },
    { label: 'Atölye',    value: 'WORKSHOP' },
    { label: 'Tiyatro',   value: 'THEATER' },
    { label: 'Teknoloji', value: 'TECH' },
    { label: 'Diğer',     value: 'OTHER' },
  ];

  statusOptions: { label: string; value: string | undefined; icon: string }[] = [
    { label: 'Tümü',       value: undefined,     icon: 'bi-grid' },
    { label: 'Yayında',    value: 'PUBLISHED',   icon: 'bi-broadcast' },
    { label: 'Sona Erdi',  value: 'ARCHIVED',    icon: 'bi-archive' },
    { label: 'Durduruldu', value: 'UNPUBLISHED', icon: 'bi-pause-circle' },
  ];

  sortOptions: { label: string; sortBy: string; direction: string }[] = [
    { label: 'En Yakın Tarih',  sortBy: 'executionDate', direction: 'ASC' },
    { label: 'En Uzak Tarih',   sortBy: 'executionDate', direction: 'DESC' },
    { label: 'En Yeni Eklenen', sortBy: 'creationDate',  direction: 'DESC' },
    { label: 'En Eski Eklenen', sortBy: 'creationDate',  direction: 'ASC' },
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
    this.participationService.getMyParticipatedEvents(
      this.currentPage(), 12,
      this.selectedSortBy(),
      this.selectedDirection(),
      this.search(),
      this.selectedCategory(),
      this.selectedStatus()
    ).subscribe({
      next: (page) => {
        this.events.set(page.content);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString('tr-TR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric'
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

  onStatusChange(value: string | undefined) {
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

  get imageUrl() {
    return (event: EventResponse) => {
      const path = event.imagePath;
      if (!path) return 'images/default_other.jpg';
      if (path.startsWith('default_')) return `images/${path}`;
      return `http://localhost:8050/uploads/event-images/${path}`;
    };
  }

  getStatusBadge(status: string): { label: string; cls: string } {
    switch (status) {
      case 'PUBLISHED':   return { label: 'Yayında',    cls: 'badge-published' };
      case 'UNPUBLISHED': return { label: 'Durduruldu', cls: 'badge-unpublished' };
      case 'ARCHIVED':    return { label: 'Sona Erdi',  cls: 'badge-archived' };
      default:            return { label: status,        cls: '' };
    }
  }

  leaveEvent(eventId: number, eventStatus: string) {
    if (eventStatus === 'ARCHIVED') return;
    this.participationService.leave(eventId).subscribe({
      next: () => this.events.set(this.events().filter(e => e.id !== eventId)),
      error: () => {}
    });
  }

  goToDetail(eventId: number) {
    this.router.navigate(['/events', eventId]);
  }
}