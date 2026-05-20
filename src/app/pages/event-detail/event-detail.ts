import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { EventService } from '../../core/services/event';
import { CommentService } from '../../core/services/comment';
import { LikeService } from '../../core/services/like';
import { ParticipationService } from '../../core/services/participation';
import { AuthService } from '../../core/services/auth';
import { EventResponse } from '../../models/event.model';
import { CommentResponse } from '../../models/comment.model';
import { UserResponse } from '../../models/user.model';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './event-detail.html',
  styleUrl: './event-detail.scss'
})
export class EventDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private eventService = inject(EventService);
  private commentService = inject(CommentService);
  private likeService = inject(LikeService);
  private participationService = inject(ParticipationService);
  private authService = inject(AuthService);

  event = signal<EventResponse | null>(null);
  comments = signal<CommentResponse[]>([]);
  loading = signal(true);
  commentLoading = signal(false);

  isLiked = signal(false);
  isParticipating = signal(false);
  likeCount = signal(0);
  participantCount = signal(0);

  // Katılımcı listesi
  participants = signal<UserResponse[]>([]);
  showParticipants = signal(false);
  confirmBanId = signal<number | null>(null);

  newCommentText = '';
  editingCommentId = signal<number | null>(null);
  editingCommentText = signal('');

  get currentUser() { return this.authService.currentUser; }

  get imageUrl(): string {
    const path = this.event()?.imagePath;
    if (!path) return 'images/default_other.jpg';
    if (path.startsWith('default_')) return `images/${path}`;
    return `http://localhost:8050/uploads/event-images/${path}`;
  }

  get categoryLabel(): string {
    const map: Record<string, string> = {
      MUSIC: 'Müzik', SPORT: 'Spor', WORKSHOP: 'Atölye',
      THEATER: 'Tiyatro', TECH: 'Teknoloji', OTHER: 'Diğer'
    };
    return map[this.event()?.category ?? ''] ?? '';
  }

  get formattedDate(): string {
    const d = this.event()?.executionDate;
    if (!d) return '';
    return new Date(d).toLocaleDateString('tr-TR', {
      day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  get isOwner(): boolean {
    return this.event()?.creator?.cid === this.currentUser?.cid;
  }

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadEvent(id);
    this.loadComments(id);
    this.loadLikeCount(id);
    this.checkParticipation(id);
    this.checkLike(id);
    this.loadParticipants(id);
  }

  loadParticipants(id: number) {
    this.participationService.getEventParticipants(id).subscribe({
      next: (users) => {
        this.participants.set(users);
        this.participantCount.set(users.length);
      },
      error: () => {}
    });
  }

  loadEvent(id: number) {
    this.eventService.getById(id).subscribe({
      next: (e) => { this.event.set(e); this.loading.set(false); },
      error: () => { this.loading.set(false); this.router.navigate(['/events']); }
    });
  }

  loadComments(id: number) {
    this.commentService.getComments(id).subscribe({
      next: (c) => this.comments.set(c),
      error: () => {}
    });
  }

  loadLikeCount(id: number) {
    this.likeService.getLikeCount(id).subscribe({
      next: (r) => this.likeCount.set(r.likeCount),
      error: () => {}
    });
  }

  checkParticipation(id: number) {
    this.participationService.getMyParticipatedEvents(0, 999).subscribe({
      next: (page) => {
        this.isParticipating.set(page.content.some(e => e.id === id));
      },
      error: () => {}
    });
  }

  checkLike(id: number) {
    this.likeService.getMyLikedEvents(0, 999).subscribe({
      next: (page) => {
        this.isLiked.set(page.content.some(e => e.id === id));
      },
      error: () => {}
    });
  }

  toggleLike() {
    const id = this.event()!.id;
    const action = this.isLiked()
      ? this.likeService.unlike(id)
      : this.likeService.like(id);

    action.subscribe({
      next: () => {
        this.isLiked.set(!this.isLiked());
        this.likeCount.set(this.likeCount() + (this.isLiked() ? 1 : -1));
      },
      error: () => {}
    });
  }

  toggleParticipation() {
    const id = this.event()!.id;
    const action = this.isParticipating()
      ? this.participationService.leave(id)
      : this.participationService.join(id);

    action.subscribe({
      next: () => {
        const joining = !this.isParticipating();
        this.isParticipating.set(joining);
        this.participantCount.set(this.participantCount() + (joining ? 1 : -1));
      },
      error: () => {}
    });
  }

  toggleParticipantList() {
    this.showParticipants.set(!this.showParticipants());
    this.confirmBanId.set(null);
  }

  confirmBan(userId: number) {
    this.confirmBanId.set(userId);
  }

  cancelBan() {
    this.confirmBanId.set(null);
  }

  banUser(userId: number) {
    const eventId = this.event()!.id;
    this.participationService.banUser(eventId, userId).subscribe({
      next: () => {
        this.participants.set(this.participants().filter(p => p.cid !== userId));
        this.participantCount.set(this.participantCount() - 1);
        this.confirmBanId.set(null);
      },
      error: () => {}
    });
  }

  addComment() {
    if (!this.newCommentText.trim()) return;
    const id = this.event()!.id;
    this.commentLoading.set(true);

    this.commentService.addComment(id, { text: this.newCommentText }).subscribe({
      next: (comment) => {
        this.comments.set([comment, ...this.comments()]);
        this.newCommentText = '';
        this.commentLoading.set(false);
      },
      error: () => this.commentLoading.set(false)
    });
  }

  startEdit(comment: CommentResponse) {
    this.editingCommentId.set(comment.id);
    this.editingCommentText.set(comment.text);
  }

  cancelEdit() {
    this.editingCommentId.set(null);
    this.editingCommentText.set('');
  }

  saveEdit(commentId: number) {
    if (!this.editingCommentText().trim()) return;

    this.commentService.updateComment(commentId, { text: this.editingCommentText() }).subscribe({
      next: (updated) => {
        this.comments.set(this.comments().map(c => c.id === commentId ? updated : c));
        this.cancelEdit();
      },
      error: () => {}
    });
  }

  deleteComment(commentId: number) {
    this.commentService.deleteComment(commentId).subscribe({
      next: () => {
        this.comments.set(this.comments().filter(c => c.id !== commentId));
      },
      error: () => {}
    });
  }

  goBack() {
    this.router.navigate(['/events']);
  }

  goToEdit() {
    this.router.navigate(['/events', this.event()!.id, 'edit']);
  }
}