import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { Router } from '@angular/router';
import { EventResponse } from '../../models/event.model';
import { LikeService } from '../../core/services/like';
import { AuthService } from '../../core/services/auth';

@Component({
  selector: 'app-event-card',
  standalone: true,
  templateUrl: './event-card.html',
  styleUrl: './event-card.scss'
})
export class EventCardComponent {
  @Input() event!: EventResponse;
  @Input() likedEventIds: Set<number> = new Set();
  @Output() likeToggled = new EventEmitter<number>();

  private router = inject(Router);
  private likeService = inject(LikeService);

  get isLiked(): boolean {
    return this.likedEventIds.has(this.event.id);
  }

  get imageUrl(): string {
    const path = this.event.imagePath;
    if (!path) return 'images/default_other.jpg';
    if (path.startsWith('default_')) return `images/${path}`;
    return `http://localhost:8050/uploads/event-images/${path}`;
  }

  get categoryLabel(): string {
    const map: Record<string, string> = {
      MUSIC: 'Müzik', SPORT: 'Spor', WORKSHOP: 'Atölye',
      THEATER: 'Tiyatro', TECH: 'Teknoloji', OTHER: 'Diğer'
    };
    return map[this.event.category] ?? this.event.category;
  }

  get formattedDate(): string {
    return new Date(this.event.executionDate).toLocaleDateString('tr-TR', {
      day: 'numeric', month: 'long', year: 'numeric'
    });
  }

  formatCount(count: number): string {
    if (count >= 1_000_000) return (count / 1_000_000).toFixed(1).replace('.0', '') + 'M';
    if (count >= 1_000) return (count / 1_000).toFixed(1).replace('.0', '') + 'B';
    return count?.toString() ?? '0';
  }

  goToDetail() {
    this.router.navigate(['/events', this.event.id]);
  }

  toggleLike(e: MouseEvent) {
    e.stopPropagation();
    const action = this.isLiked
      ? this.likeService.unlike(this.event.id)
      : this.likeService.like(this.event.id);

    action.subscribe({
      next: () => this.likeToggled.emit(this.event.id),
      error: () => {}
    });
  }
}