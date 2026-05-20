import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../../models/page.model';
import { EventResponse, ECategory } from '../../models/event.model';

@Injectable({ providedIn: 'root' })
export class ParticipationService {
  private http = inject(HttpClient);
  private readonly API = 'http://localhost:8050';

  join(eventId: number): Observable<any> {
    return this.http.post(`${this.API}/participation/${eventId}/join`, {});
  }

  leave(eventId: number): Observable<any> {
    return this.http.delete(`${this.API}/participation/${eventId}/leave`);
  }

  getMyParticipatedEvents(
  page = 0, size = 12, sortBy = 'executionDate',
  direction = 'DESC', search = '', 
  category?: ECategory,
  status?: string
  ): Observable<Page<EventResponse>> {
    let params = new HttpParams()
      .set('page', page).set('size', size)
      .set('sortBy', sortBy).set('direction', direction)
      .set('search', search);
    if (category) params = params.set('category', category);
    if (status) params = params.set('status', status);
    return this.http.get<Page<EventResponse>>(`${this.API}/participation/my-participations`, { params });
  }

  getEventParticipants(eventId: number): Observable<any[]> {
  return this.http.get<any[]>(`${this.API}/participation/${eventId}/users`);
  }

  banUser(eventId: number, userId: number): Observable<any> {
  return this.http.delete(`${this.API}/participation/${eventId}/ban/${userId}`);
  }
}