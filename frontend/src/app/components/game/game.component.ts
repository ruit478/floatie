import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of, map, startWith, Observable } from 'rxjs';
import {PetService} from '../../services/pet.service';

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
  private petService = inject(PetService);

  readonly username = signal(this.authService.getUsername());

  readonly petState = toSignal<PetState>(
    this.petService.getPetInfo().pipe(
      map((pet): PetState => ({ status: 'loaded', pet })),
      catchError((): Observable<PetState> => of({
        status: 'error',
        message: 'Could not load pet. Please try again later.'
      })),
      startWith<PetState>({ status: 'loading' })
    ),
    { requireSync: true }
  );

  // Computed signals for different UI states
  readonly pet = computed(() => {
    const state = this.petState();
    return state.status === 'loaded' ? state.pet : null;
  });

  readonly spriteUrl = computed(() => {
    const pet = this.pet();
    return pet?.spriteBase64
      ? `data:image/png;base64,${pet.spriteBase64}`
      : null;
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
