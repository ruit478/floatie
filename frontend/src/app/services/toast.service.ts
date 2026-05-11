import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  type: 'info' | 'success' | 'warning';
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 0;
  readonly toasts = signal<Toast[]>([]);

  show(message: string, type: Toast['type'] = 'info'): void {
    const id = this.nextId++;
    this.toasts.update(t => [...t, { id, message, type }]);
    setTimeout(() => this.dismiss(id), 4000);
  }

  dismiss(id: number): void {
    this.toasts.update(t => t.filter(toast => toast.id !== id));
  }
}
