package com.revature.user.dto.response;

/**
 * Response for GET /api/users/dashboard.
 * Field names MUST match the frontend DashboardResponse TypeScript model.
 */
public class DashboardResponse {

    private int totalVaultEntries;
    private int totalFavorites;
    private int trashCount;

    public DashboardResponse() {}

    public DashboardResponse(int totalVaultEntries, int totalFavorites, int trashCount) {
        this.totalVaultEntries = totalVaultEntries;
        this.totalFavorites = totalFavorites;
        this.trashCount = trashCount;
    }

    public int getTotalVaultEntries() { return totalVaultEntries; }
    public void setTotalVaultEntries(int totalVaultEntries) { this.totalVaultEntries = totalVaultEntries; }

    public int getTotalFavorites() { return totalFavorites; }
    public void setTotalFavorites(int totalFavorites) { this.totalFavorites = totalFavorites; }

    public int getTrashCount() { return trashCount; }
    public void setTrashCount(int trashCount) { this.trashCount = trashCount; }
}
