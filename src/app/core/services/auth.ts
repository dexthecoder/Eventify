import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { UserResponse, UserLoginRequest, UserRegisterRequest } from '../../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private readonly API = 'http://localhost:8050';

  private currentUserSubject = new BehaviorSubject<UserResponse | null>(null);
  private initializedSubject = new BehaviorSubject<boolean>(false);

  currentUser$ = this.currentUserSubject.asObservable();
  initialized$ = this.initializedSubject.asObservable();

  constructor() {
    // Sayfa yenilendiğinde backend session'ını kontrol et
    this.http.get<UserResponse>(`${this.API}/user/me`).subscribe({
      next: (user) => {
        this.currentUserSubject.next(user);
        this.initializedSubject.next(true);
      },
      error: () => {
        this.currentUserSubject.next(null);
        this.initializedSubject.next(true);
      }
    });
  }

  register(data: UserRegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.API}/user/register`, data);
  }

  login(data: UserLoginRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.API}/user/login`, data).pipe(
      tap(user => this.currentUserSubject.next(user))
    );
  }

  logout(): Observable<void> {
    return this.http.get<void>(`${this.API}/user/logout`).pipe(
      tap(() => this.currentUserSubject.next(null))
    );
  }

  get currentUser(): UserResponse | null {
    return this.currentUserSubject.value;
  }

  isLoggedIn(): boolean {
    return this.currentUserSubject.value !== null;
  }
}