import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { HttpClient } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import {catchError, of, map, startWith, Observable} from 'rxjs';

interface PetInfo {
  id: string;
  name: string;
  classType: string;
  subclass: string;
  colorHex: string;
  expression: string;
  spriteBase64: string;
  hunger: number;
  happiness: number;
  energy: number;
  health: number;
  hygiene: number;
  weight: number;
  xp: number;
  level: number;
  lifeStage: string;
  ageDays: number;
  evolutionStage: number;
  evolutionPath: string;
  isSick: boolean;
  isAsleep: boolean;
  bondLevel: number;
  personality: string | null;
}

type PetState =
  | { status: 'loading' }
  | { status: 'loaded'; pet: PetInfo }
  | { status: 'error'; message: string };

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game.component.html',
  styleUrl: './game.component.css'
})
export class GameComponent {
  private authService = inject(AuthService);
  private http = inject(HttpClient);

  readonly username = signal(this.authService.getUsername());
  readonly spriteUrl = computed(() => {
    const pet = this.pet();
    return pet?.spriteBase64
      ? `data:image/png;base64,${pet.spriteBase64}`
      : null;
  });
  // requireSync is safe here because startWith guarantees
  // a synchronous emission — the signal is never undefined.
  readonly petState = toSignal<PetState>(
    this.http.get<PetInfo>('http://localhost:8080/api/v1/pet/sprite/info').pipe(
      map((pet): PetState => ({ status: 'loaded', pet })),
      catchError((): Observable<PetState> => of({ status: 'error', message: 'Could not load pet.' })),
      startWith<PetState>({ status: 'loading' })
    ),
    { requireSync: true }
  );

  readonly pet = computed(() => {
    const state = this.petState();
    return state.status === 'loaded' ? state.pet : null;
  });

  readonly isLoading = computed(() => this.petState().status === 'loading');

  readonly error = computed(() => {
    const state = this.petState();
    return state.status === 'error' ? state.message : null;
  });

  readonly coreStats = computed(() => {
    const pet = this.pet();
    if (!pet) return [];
    return [
      { label: 'Health',    value: pet.health,    icon: '❤️' },
      { label: 'Hunger',    value: pet.hunger,    icon: '🍖' },
      { label: 'Happiness', value: pet.happiness, icon: '😊' },
      { label: 'Energy',    value: pet.energy,    icon: '⚡' },
      { label: 'Hygiene',   value: pet.hygiene,   icon: '🚿' },
    ];
  });

  logout() {
    this.authService.logout();
  }
}
