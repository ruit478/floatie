export interface User {
  username: string;
  token: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  message: string;
}

export interface RegisterRequest {
  username: string;
  email?: string;  // Make optional since your backend might not use it yet
  password: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}
