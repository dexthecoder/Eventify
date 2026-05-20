import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CommentResponse, CommentRequest } from '../../models/comment.model';

@Injectable({ providedIn: 'root' })
export class CommentService {
  private http = inject(HttpClient);
  private readonly API = 'http://localhost:8050';

  getComments(eventId: number): Observable<CommentResponse[]> {
    return this.http.get<CommentResponse[]>(`${this.API}/comment/${eventId}`);
  }

  addComment(eventId: number, data: CommentRequest): Observable<CommentResponse> {
    return this.http.post<CommentResponse>(`${this.API}/comment/${eventId}`, data);
  }

  updateComment(commentId: number, data: CommentRequest): Observable<CommentResponse> {
    return this.http.put<CommentResponse>(`${this.API}/comment/${commentId}`, data);
  }

  deleteComment(commentId: number): Observable<any> {
    return this.http.delete(`${this.API}/comment/${commentId}`);
  }
}