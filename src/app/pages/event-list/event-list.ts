import { Component, inject, signal, OnInit, HostListener } from '@angular/core';
import { EventService } from '../../core/services/event';
import { LikeService } from '../../core/services/like';
import { EventResponse, ECategory } from '../../models/event.model';
import { EventCardComponent } from '../../shared/event-card/event-card';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [EventCardComponent],
  templateUrl: './event-list.html',
  styleUrl: './event-list.scss'
})
export class EventListComponent implements OnInit {
  private eventService = inject(EventService);
  private likeService = inject(LikeService);

  events = signal<EventResponse[]>([]);
  likedEventIds = signal<Set<number>>(new Set());
  loading = signal(true);
  totalPages = signal(0);
  currentPage = signal(0);
  sortDropdownOpen = signal(false);

  search = signal('');
  selectedCategory = signal<ECategory | undefined>(undefined);
  selectedSortBy = signal('executionDate');
  selectedDirection = signal('DESC');

  categories: { label: string; value: ECategory | undefined }[] = [
    { label: 'Tümü',      value: undefined },
    { label: 'Müzik',     value: 'MUSIC' },
    { label: 'Spor',      value: 'SPORT' },
    { label: 'Atölye',    value: 'WORKSHOP' },
    { label: 'Tiyatro',   value: 'THEATER' },
    { label: 'Teknoloji', value: 'TECH' },
    { label: 'Diğer',     value: 'OTHER' },
  ];

  sortOptions: { label: string; sortBy: string; direction: string }[] = [
    { label: 'En Yakın Tarih',  sortBy: 'executionDate', direction: 'ASC' },
    { label: 'En Uzak Tarih',   sortBy: 'executionDate', direction: 'DESC' },
    { label: 'En Yeni Eklenen', sortBy: 'creationDate',  direction: 'DESC' },
    { label: 'En Eski Eklenen', sortBy: 'creationDate',  direction: 'ASC' },
  ];

  ngOnInit() {
    this.loadLikedEvents();
    this.loadEvents();
  }

  loadEvents() {
    this.loading.set(true);
    this.eventService.getAll(
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

  activeSortLabel(): string {
    const active = this.sortOptions.find(o =>
      o.sortBy === this.selectedSortBy() && o.direction === this.selectedDirection()
    );
    return active?.label ?? 'Sırala';
  }

  loadLikedEvents() {
    this.likeService.getMyLikedEvents(0, 999).subscribe({
      next: (page) => {
        this.likedEventIds.set(new Set(page.content.map(e => e.id)));
      },
      error: () => {}
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.sort-dropdown-wrap')) {
      this.sortDropdownOpen.set(false);
    }
  }

  onLikeToggled(eventId: number) {
    const current = new Set(this.likedEventIds());
    const isNowLiked = !current.has(eventId);
    if (isNowLiked) current.add(eventId);
    else current.delete(eventId);
    this.likedEventIds.set(current);

    this.events.set(
      this.events().map(e =>
        e.id === eventId
          ? { ...e, likeCount: e.likeCount + (isNowLiked ? 1 : -1) }
          : e
      )
    );
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

  goToPage(page: number) {
    this.currentPage.set(page);
    this.loadEvents();
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }
}