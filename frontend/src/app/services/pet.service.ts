import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {PetInfo} from '../models/pet.models';

@Injectable({
  providedIn: 'root'
})
export class PetService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/pet`;

  getPetInfo(): Observable<PetInfo> {
    return this.http.get<PetInfo>(`${this.baseUrl}/info`);
  }

  getStatus(): Observable<PetInfo> {
    return this.http.get<PetInfo>(`${this.baseUrl}/status`);
  }

  feed(): Observable<PetInfo> {
    return this.http.post<PetInfo>(`${this.baseUrl}/feed`, {});
  }

  play(): Observable<PetInfo> {
    return this.http.post<PetInfo>(`${this.baseUrl}/play`, {});
  }

  rest(): Observable<PetInfo> {
    return this.http.post<PetInfo>(`${this.baseUrl}/rest`, {});
  }

  clean(): Observable<PetInfo> {
    return this.http.post<PetInfo>(`${this.baseUrl}/clean`, {});
  }

  heal(): Observable<PetInfo> {
    return this.http.post<PetInfo>(`${this.baseUrl}/heal`, {});
  }

  sleep(): Observable<PetInfo> {
    return this.http.post<PetInfo>(`${this.baseUrl}/sleep`, {});
  }

  wake(): Observable<PetInfo> {
    return this.http.post<PetInfo>(`${this.baseUrl}/wake`, {});
  }

  replacePet(): Observable<PetInfo> {
    return this.http.post<PetInfo>(`${this.baseUrl}/replace`, {});
  }
}
