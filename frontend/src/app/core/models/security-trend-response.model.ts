export interface TrendDataPoint {
  recordedAt: string;
  overallScore: number;
  weakPasswordsCount: number;
  reusedPasswordsCount: number;
  oldPasswordsCount: number;
}

export interface SecurityTrendResponse {
  trendPoints: TrendDataPoint[];
  scoreChange: number;
  trendDirection: string; // 'UP' | 'DOWN' | 'STABLE'
  periodLabel: string;
}
