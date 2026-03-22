export interface ReusedEntryInfo {
  entryId: number;
  title: string;
  username: string;
  websiteUrl: string;
}

export interface ReusedPasswordGroup {
  reuseCount: number;
  entries: ReusedEntryInfo[];
}

export interface ReusedPasswordResponse {
  totalReusedGroups: number;
  totalAffectedEntries: number;
  reusedGroups: ReusedPasswordGroup[];
}
