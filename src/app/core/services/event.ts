import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventResponse, EventCreateRequest, EventUpdateRequest, ECategory } from '../../models/event.model';
import { Page } from '../../models/page.model';

@Injectable({ providedIn: 'root' })
export class EventService {
  private http = inject(HttpClient);
  private readonly API = 'http://localhost:8050';

  getAll(page = 0, size = 12, sortBy = 'executionDate', direction = 'ASC', search = '', category?: ECategory): Observable<Page<EventResponse>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('direction', direction)
      .set('search', search);
    if (category) params = params.set('category', category);
    return this.http.get<Page<EventResponse>>(`${this.API}/event/list`, { params });
  }

  getById(id: number): Observable<EventResponse> {
    return this.http.get<EventResponse>(`${this.API}/event/${id}`);
  }

  getMyEvents(page = 0, size = 12, sortBy = 'creationDate', direction = 'DESC', search = '', category?: ECategory): Observable<Page<EventResponse>> {
    let params = new HttpParams()
      .set('page', page).set('size', size)
      .set('sortBy', sortBy).set('direction', direction)
      .set('search', search);
    if (category) params = params.set('category', category);
    return this.http.get<Page<EventResponse>>(`${this.API}/event/my-events`, { params });
  }

  create(data: EventCreateRequest, file?: File): Observable<EventResponse> {
    const formData = new FormData();
    Object.entries(data).forEach(([k, v]) => formData.append(k, v));
    if (file) formData.append('image', file);
    return this.http.post<EventResponse>(`${this.API}/event/create`, formData);
  }

  update(id: number, data: EventUpdateRequest, file?: File): Observable<any> {
    const formData = new FormData();
    Object.entries(data).forEach(([k, v]) => { if (v !== undefined) formData.append(k, v); });
    if (file) formData.append('image', file);
    return this.http.put(`${this.API}/event/update/${id}`, formData);
  }

  delete(id: number): Observable<any> {
    return this.http.delete(`${this.API}/event/delete/${id}`);
  }
}