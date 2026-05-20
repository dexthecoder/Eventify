import { Component, inject, signal, OnInit, HostListener } from '@angular/core';
import { LikeService } from '../../core/services/like';
import { EventResponse, ECategory } from '../../models/event.model';
import { EventCardComponent } from '../../shared/event-card/event-card';

@Component({
  selector: 'app-my-likes',
  standalone: true,
  imports: [EventCardComponent],
  templateUrl: './my-likes.html',
  styleUrl: './my-likes.scss'
})
export class MyLikesComponent implements OnInit {
  private likeService = inject(LikeService);

  events = signal<EventResponse[]>([]);
  likedEventIds = signal<Set<number>>(new Set());
  loading = signal(true);
  totalPages = signal(0);
  currentPage = signal(0);

  search = signal('');
  selectedCategory = signal<ECategory | undefined>(undefined);
  selectedSortBy = signal('executionDate');
  selectedDirection = signal('ASC');
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

  ngOnInit() {
    this.loadEvents();
  }

  loadEvents() {
    this.loading.set(true);
    this.likeService.getMyLikedEvents(
      this.currentPage(), 12,
      this.selectedSortBy(),
      this.selectedDirection(),
      this.search(),
      this.selectedCategory()
    ).subscribe({
      next: (page) => {
        this.events.set(page.content);
        this.totalPages.set(page.totalPages);
        const ids = new Set(page.content.map(e => e.id));
        this.likedEventIds.set(ids);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onLikeToggled(eventId: number) {
    // Beğeni geri çekilince listeden kaldır
    this.events.set(this.events().filter(e => e.id !== eventId));
    const current = new Set(this.likedEventIds());
    current.delete(eventId);
    this.likedEventIds.set(current);
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

  activeSortLabel(): string {
    const active = this.sortOptions.find(o =>
      o.sortBy === this.selectedSortBy() && o.direction === this.selectedDirection()
    );
    return active?.label ?? 'Sırala';
  }

  goToPage(page: number) {
    this.currentPage.set(page);
    this.loadEvents();
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }
}