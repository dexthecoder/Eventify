import { UserResponse } from './user.model';

export interface CommentResponse {
  id: number;
  text: string;
  creationDate: string;
  updatedDate: string;
  creator: UserResponse;
}

export interface CommentRequest {
  text: string;
}