import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { PetService } from '../../services/pet.service';
import { ToastService } from '../../services/toast.service';
import { PetInfo } from '../../models/pet.models';
import { DeathModalComponent } from '../death-modal/death-modal.component';
import { ToastComponent } from '../toast/toast.component';

type PetState =
  | { status: 'loading' }
  | { status: 'loaded'; pet: PetInfo }
  | { status: 'error'; message: string };

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [CommonModule, DeathModalComponent, ToastComponent],
  templateUrl: './game.component.html',
  styleUrl: './game.component.css'
})
export class GameComponent implements OnInit {
  private authService = inject(AuthService);
  private petService = inject(PetService);
  private toastService = inject(ToastService);

  readonly username = signal(this.authService.getUsername());

  readonly petState = signal<PetState>({ status: 'loading' });
  readonly actionInProgress = signal<string | null>(null);

  ngOnInit(): void {
    this.loadPet();
  }

  // ── Computed ──

  readonly pet = computed(() => {
    const state = this.petState();
    return state.status === 'loaded' ? state.pet : null;
  });

  readonly spriteUrl = computed(() => {
    const p = this.pet();
    return p?.spriteBase64 ? `data:image/png;base64,${p.spriteBase64}` : null;
  });

  readonly isLoading = computed(() => this.petState().status === 'loading');

  readonly error = computed(() => {
    const state = this.petState();
    return state.status === 'error' ? state.message : null;
  });

  readonly coreStats = computed(() => {
    const p = this.pet();
    if (!p) return [];
    return [
      { label: 'Health',    value: Math.min(100, p.health),    icon: '❤️' },
      { label: 'Hunger',    value: Math.min(100, p.hunger),    icon: '🍖' },
      { label: 'Happiness', value: Math.min(100, p.happiness), icon: '😊' },
      { label: 'Energy',    value: Math.min(100, p.energy),    icon: '⚡' },
      { label: 'Hygiene',   value: Math.min(100, p.hygiene),   icon: '🚿' },
    ];
  });

  readonly isBusy = computed(() => this.actionInProgress() !== null);
  readonly isDead = computed(() => this.pet()?.lifeStage === 'DEAD');

  readonly canFeed  = computed(() => { const p = this.pet(); return p && p.hunger < 100 && !p.isAsleep && !this.isBusy() && !this.isDead(); });
  readonly canPlay  = computed(() => { const p = this.pet(); return p && p.energy > 0 && !p.isAsleep && !this.isBusy() && !this.isDead(); });
  readonly canRest  = computed(() => { const p = this.pet(); return p && p.energy < 100 && !this.isBusy() && !this.isDead(); });
  readonly canClean = computed(() => { const p = this.pet(); return p && p.hygiene < 100 && !this.isBusy() && !this.isDead(); });
  readonly canHeal  = computed(() => { const p = this.pet(); return p && p.health < 100 && !this.isBusy() && !this.isDead(); });

  // ── Actions ──

  loadPet(): void {
    this.petState.set({ status: 'loading' });
    this.petService.getPetInfo().subscribe({
      next: (pet) => this.petState.set({ status: 'loaded', pet }),
      error: () => this.petState.set({ status: 'error', message: 'Could not load pet.' })
    });
  }

  feed(): void    { this.doAction('feed', this.petService.feed()); }
  play(): void    { this.doAction('play', this.petService.play()); }
  rest(): void    { this.doAction('rest', this.petService.rest()); }
  clean(): void   { this.doAction('clean', this.petService.clean()); }
  heal(): void    { this.doAction('heal', this.petService.heal()); }
  toggleSleep(): void {
    const p = this.pet();
    if (!p) return;
    const action = p.isAsleep ? this.petService.wake() : this.petService.sleep();
    this.doAction(p.isAsleep ? 'wake' : 'sleep', action);
  }

  private doAction(label: string, request$: import('rxjs').Observable<PetInfo>): void {
    if (this.isBusy()) return;
    this.actionInProgress.set(label);
    const oldPet = this.pet();
    request$.subscribe({
      next: (pet) => {
        this.detectEvents(oldPet, pet);
        this.petState.set({ status: 'loaded', pet });
        this.actionInProgress.set(null);
      },
      error: () => {
        this.actionInProgress.set(null);
      }
    });
  }

  private detectEvents(old: PetInfo | null, next: PetInfo): void {
    if (!old) return;
    if (old.lifeStage !== next.lifeStage) {
      this.toastService.show(`${old.lifeStage} → ${next.lifeStage}`, 'success');
    }
    if (old.evolutionStage !== next.evolutionStage) {
      this.toastService.show(`Evolution stage ${next.evolutionStage} (${next.evolutionPath})`, 'success');
    } else if (old.level !== next.level) {
      this.toastService.show(`Level up! Now level ${next.level}`, 'info');
    }
  }

  logout(): void {
    this.authService.logout();
  }

  replacePet(): void {
    this.petState.set({ status: 'loading' });
    this.petService.replacePet().subscribe({
      next: (pet) => {
        this.petState.set({ status: 'loaded', pet });
        this.toastService.show(`A new ${pet.subclass} named ${pet.name} has arrived!`, 'success');
      },
      error: () => this.petState.set({ status: 'error', message: 'Could not create a new pet.' })
    });
  }

  deleteAccount(): void {
    this.authService.deleteAccount().subscribe();
  }
}
