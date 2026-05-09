import { Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

const DEFAULT_MESSAGES: Record<string, (err: any) => string> = {
  required:  ()    => '⚠ This field is required',
  minlength: (e)   => `⚠ Minimum ${e.requiredLength} characters`,
  maxlength: (e)   => `⚠ Maximum ${e.requiredLength} characters`,
  email:     ()    => '⚠ Please enter a valid email',
};

@Component({
  selector: 'field-error',
  standalone: true,
  styleUrl: './field-error.component.css',
  templateUrl : 'field-error.component.html',
})
export class FieldErrorComponent {
  control = input.required<AbstractControl>();
  messages = input<Record<string, string>>({});

  errorKeys(): string[] {
    return Object.keys(this.control().errors ?? {});
  }

  message(key: string): string {
    const custom = this.messages()[key];
    if (custom) return custom;
    const fn = DEFAULT_MESSAGES[key];
    return fn ? fn(this.control().errors![key]) : `⚠ Invalid field`;
  }
}
