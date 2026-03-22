export interface PasswordCategoryBreakdown {
  categoryName: string;
  count: number;
  averageScore: number;
  weakCount: number;
}

export interface PasswordHealthMetricsResponse {
  totalPasswords: number;
  strongCount: number;
  goodCount: number;
  fairCount: number;
  weakCount: number;
  veryWeakCount: number;
  averageStrengthScore: number;
  categoryBreakdowns: PasswordCategoryBreakdown[];
}
