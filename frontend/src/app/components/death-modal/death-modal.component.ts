import { Component, Output, EventEmitter, ViewChild, ElementRef, AfterViewInit } from '@angular/core';

@Component({
  selector: 'app-death-modal',
  standalone: true,
  templateUrl: './death-modal.component.html',
  styleUrl: './death-modal.component.css'
})
export class DeathModalComponent implements AfterViewInit {
  @Output() replace = new EventEmitter<void>();
  @Output() deleteAccount = new EventEmitter<void>();

  @ViewChild('firstBtn') firstBtn?: ElementRef<HTMLButtonElement>;

  ngAfterViewInit(): void {
    this.firstBtn?.nativeElement.focus();
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
    }
    // Trap focus within the modal
    if (event.key === 'Tab') {
      const modal = (event.currentTarget as HTMLElement).querySelector('.modal-card');
      if (!modal) return;
      const focusable = modal.querySelectorAll<HTMLElement>(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      );
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey) {
        if (document.activeElement === first) {
          event.preventDefault();
          last.focus();
        }
      } else {
        if (document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }
    }
  }
}
