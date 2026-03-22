export interface SecurityScoreResponse {
  overallScore: number;
  scoreLabel: string;
  totalPasswords: number;
  strongPasswords: number;
  fairPasswords: number;
  weakPasswords: number;
  reusedPasswords: number;
  oldPasswords: number;
  recommendation: string;
}
