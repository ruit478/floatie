// frontend/src/app/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/auth.models';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {
    this.loadStoredUser();
  }

  private loadStoredUser(): void {
    const token = localStorage.getItem('access_token');
    const username = localStorage.getItem('username');

    if (token && username && !this.isTokenExpired(token)) {
      this.currentUserSubject.next({ username, token });
    }
  }

  register(userData: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, userData)
      .pipe(
        tap(response => this.handleAuthResponse(response)),
        catchError((err) => this.handleError(err))
      );
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, credentials)
      .pipe(
        tap(response => this.handleAuthResponse(response)),
        catchError((err) => this.handleError(err))
      );
  }

  logout(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('username');
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  private handleAuthResponse(response: AuthResponse): void {
    // Store token and username from backend response
    localStorage.setItem('access_token', response.token);
    localStorage.setItem('username', response.username);

    const { username, token } = response;
    this.currentUserSubject.next({ username, token });
  }

  private readonly errorMessages: Record<number, string> = {
    401: 'Invalid credentials',
    409: 'Username already exists',
    400: 'Invalid input',
  };

  private handleError(error: any): Observable<never> {
    console.log('Error body:', error.error);
    const message = error.error?.message ?? this.errorMessages[error.status] ?? 'An error occurred';
    return throwError(() => new Error(message));
  }

  isAuthenticated(): boolean {
    const user = this.currentUserSubject.value;
    return !!user && !this.isTokenExpired(user.token);
  }

  getToken(): string | null {
    return this.currentUserSubject.value?.token ?? null;
  }
  getUsername(): string | null {
    return this.currentUserSubject.value?.username ?? null;
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload));
      const exp = decoded.exp;
      if (!exp) return true;
      return Date.now() >= exp * 1000;
    } catch {
      return true;
    }
  }
}
