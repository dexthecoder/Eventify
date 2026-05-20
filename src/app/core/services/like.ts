import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../../models/page.model';
import { EventResponse, ECategory } from '../../models/event.model';

@Injectable({ providedIn: 'root' })
export class LikeService {
  private http = inject(HttpClient);
  private readonly API = 'http://localhost:8050';

  like(eventId: number): Observable<any> {
    return this.http.post(`${this.API}/like/${eventId}`, {});
  }

  unlike(eventId: number): Observable<any> {
    return this.http.delete(`${this.API}/like/${eventId}`);
  }

  getLikeCount(eventId: number): Observable<{ eventId: number; likeCount: number }> {
    return this.http.get<any>(`${this.API}/like/count/${eventId}`);
  }

  getMyLikedEvents(page = 0, size = 12, sortBy = 'executionDate', direction = 'ASC', search = '', category?: ECategory): Observable<Page<EventResponse>> {
    let params = new HttpParams()
      .set('page', page).set('size', size)
      .set('sortBy', sortBy).set('direction', direction)
      .set('search', search);
    if (category) params = params.set('category', category);
    return this.http.get<Page<EventResponse>>(`${this.API}/like/my-likes`, { params });
  }
}