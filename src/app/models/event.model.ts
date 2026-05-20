import { UserResponse } from './user.model';

export type ECategory = 'MUSIC' | 'SPORT' | 'WORKSHOP' | 'THEATER' | 'TECH' | 'OTHER';
export type EStatus = 'PUBLISHED' | 'UNPUBLISHED' | 'ARCHIVED';

export interface EventResponse {
  id: number;
  title: string;
  description: string;
  creationDate: string;
  executionDate: string;
  location: string;
  category: ECategory;
  status: EStatus;
  imagePath: string;
  creator: UserResponse;
  likeCount: number;
  participantCount: number;
}

export interface EventCreateRequest {
  title: string;
  description: string;
  executionDate: string;
  location: string;
  category: ECategory;
}

export interface EventUpdateRequest {
  title?: string;
  description?: string;
  executionDate?: string;
  location?: string;
  category?: ECategory;
  status?: EStatus;
}