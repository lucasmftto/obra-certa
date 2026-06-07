export type MemberRole = 'VIEWER' | 'EDITOR';

export interface MemberResponse {
  userId: number;
  name: string;
  email: string;
  role: MemberRole;
}

export interface MemberRequest {
  email: string;
  role: MemberRole;
}
