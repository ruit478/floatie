import { Component, signal } from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, ActivatedRoute, RouterModule} from '@angular/router';
import { AuthService } from '../../services/auth.service';
import {CommonModule} from '@angular/common';
import { FieldErrorComponent } from '../shared/field-error.component';

@Component({
  selector: 'app-login',
  standalone: true,  // <-- KEY: Makes this a standalone component
  imports: [
    CommonModule,      // Provides ngIf, ngFor, etc.
    ReactiveFormsModule, // Provides form functionality
    RouterModule,       // Provides routerLink
    FieldErrorComponent
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  loginForm: FormGroup;
  isLoading = signal(false);
  errorMessage = signal('');
  returnUrl: string = '/game';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/game';
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        this.isLoading.set(false)
        console.log('Login successful:', response.message);
        this.router.navigate([this.returnUrl]);
      },
      error: (error) => {
        this.errorMessage.set(error.message || 'Login failed. Please try again.');
        this.isLoading.set(false);
      }
    });
  }
}
