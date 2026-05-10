import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PetService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/pet`;

  /**
   * Fetch the authenticated user's pet info, including the sprite as a
   * Base64-encoded PNG string.
   */
  getPetInfo(): Observable<PetInfo> {
    return this.http.get<PetInfo>(`${this.baseUrl}/sprite/info`);
  }
}
