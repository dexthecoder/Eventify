export interface UserResponse {
  cid: number;
  name: string;
  surname: string;
  email: string;
  phone: string;
}

export interface UserLoginRequest {
  username: string;
  password: string;
}

export interface UserRegisterRequest {
  name: string;
  surname: string;
  email: string;
  phone: string;
  password: string;
}