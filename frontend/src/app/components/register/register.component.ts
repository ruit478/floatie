// frontend/src/app/components/register/register.component.ts
import { Component, inject, DestroyRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { RegisterRequest } from '../../models/auth.models';
import { FieldErrorComponent } from '../shared/field-error.component';
import { Subject, of, concat } from 'rxjs';
import { switchMap, catchError, tap, map } from 'rxjs/operators';
import {takeUntilDestroyed, toSignal} from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterModule,
    FieldErrorComponent
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  registerForm: FormGroup;
  private submitSubject = new Subject<RegisterRequest>();

  // Conversion of the registration observable to a signal
  registrationState = toSignal(
    this.submitSubject.pipe(
      switchMap((payload) =>
        concat(
          of({ loading: true, error: null } as const),
          this.authService.register(payload).pipe(
            tap(() => this.router.navigate(['/game'])),
            map(() => ({ loading: false, error: null } as const)),
            catchError((error) => {
              return of({ loading: false, error: error.message || 'Registration failed. Please try again.' } as const);
            })
          )
        )
      ),
      takeUntilDestroyed(this.destroyRef)
    ),
    { initialValue: { loading: false, error: null } as const }
  );

  constructor() {
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

    const { confirmPassword, ...rest } = this.registerForm.value;
    const payload: RegisterRequest = {
      username: rest.username,
      password: rest.password,
      ...(rest.email ? { email: rest.email } : {}),
    };

    this.submitSubject.next(payload);
  }

  isFieldInvalid(fieldName: string): boolean {
    const control = this.registerForm.get(fieldName);
    return control ? (control.invalid && (control.touched || control.dirty)) : false;
  }

  getFieldErrorId(fieldName: string): string {
    return `${fieldName}-error`;
  }
}
