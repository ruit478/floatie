// frontend/src/app/components/register/register.component.ts
import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import {AuthService} from '../../services/auth.service';
import {RegisterRequest} from '../../models/auth.models';
import {FieldErrorComponent} from '../shared/field-error.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    FieldErrorComponent
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  registerForm: FormGroup;
  isLoading = signal(false);
  errorMessage = signal('');

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      email: ['', [Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
    }, {
      validators: this.passwordMatchValidator.bind(this)
    });
  }

  passwordMatchValidator(group: AbstractControl): null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword');

    if (password && confirmPassword?.value && password !== confirmPassword.value) {
      confirmPassword.setErrors({ ...confirmPassword.errors, passwordMismatch: true });
    } else {
      const errors = { ...confirmPassword?.errors };
      delete errors['passwordMismatch'];
      confirmPassword?.setErrors(Object.keys(errors).length ? errors : null);
    }

    return null;
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');


    const { confirmPassword, ...rest } = this.registerForm.value;
    const payload: RegisterRequest = {
      username: rest.username,
      password: rest.password,
      ...(rest.email ? { email: rest.email } : {}),
    };

    this.authService.register(payload).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/game']);
      },
      error: (error) => {
        this.errorMessage.set(error.message || 'Registration failed. Please try again.');
        this.isLoading.set(false);
      }
    });
  }
}
