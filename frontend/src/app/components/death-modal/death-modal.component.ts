import { Component, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-death-modal',
  standalone: true,
  templateUrl: './death-modal.component.html',
  styleUrl: './death-modal.component.css'
})
export class DeathModalComponent {
  @Output() replace = new EventEmitter<void>();
  @Output() deleteAccount = new EventEmitter<void>();
}
