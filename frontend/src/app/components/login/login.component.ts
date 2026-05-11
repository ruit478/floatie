// frontend/src/app/components/login/login.component.ts
import { Component, inject, DestroyRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { FieldErrorComponent } from '../shared/field-error.component';
import { Subject, of, concat } from 'rxjs';
import { switchMap, catchError, tap, map } from 'rxjs/operators';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterModule,
    FieldErrorComponent
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);

  loginForm: FormGroup;
  private submitSubject = new Subject<{ username: string; password: string }>();
  returnUrl: string = '/game';

  // Conversion of the login observable to a signal
  loginState = toSignal(
    this.submitSubject.pipe(
      switchMap((credentials) =>
        concat(
          of({ loading: true, error: null } as const),
          this.authService.login(credentials).pipe(
            tap(() => this.router.navigate([this.returnUrl])),
            map(() => ({ loading: false, error: null } as const)),
            catchError((error) => {
              return of({
                loading: false,
                error: error.message || 'Login failed. Please try again.'
              } as const);
            })
          )
        )
      ),
      takeUntilDestroyed(this.destroyRef)
    ),
    { initialValue: { loading: false, error: null } as const }
  );

  constructor() {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    const returnUrl = this.route.snapshot.queryParams['returnUrl'];
    this.returnUrl = (returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//'))
      ? returnUrl
      : '/game';
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.submitSubject.next(this.loginForm.value);
  }

  isFieldInvalid(fieldName: string): boolean {
    const control = this.loginForm.get(fieldName);
    return control ? (control.invalid && (control.touched || control.dirty)) : false;
  }

  getFieldErrorId(fieldName: string): string {
    return `${fieldName}-error`;
  }
}
