export interface AgeDistributionBucket {
  label: string;
  count: number;
  minDays: number;
  maxDays: number;
}

export interface PasswordAgeResponse {
  totalPasswords: number;
  freshCount: number;
  agingCount: number;
  oldCount: number;
  ancientCount: number;
  averageAgeInDays: number;
  distribution: AgeDistributionBucket[];
}
