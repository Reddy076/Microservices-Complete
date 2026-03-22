import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VaultService } from '../../core/api/api/vault.service';
import { CategoryControllerService } from '../../core/api/api/categoryController.service';
import { FolderControllerService } from '../../core/api/api/folderController.service';
import { VaultEntryResponse } from '../../core/api/model/vaultEntryResponse';
import { VaultEntryDetailResponse } from '../../core/api/model/vaultEntryDetailResponse';
import { VaultEntryRequest } from '../../core/api/model/vaultEntryRequest';
import { TrashEntryResponse } from '../../core/api/model/trashEntryResponse';
import { CategoryDTO } from '../../core/api/model/categoryDTO';
import { FolderDTO } from '../../core/api/model/folderDTO';
import { SnapshotResponse } from '../../core/api/model/snapshotResponse';
import { ShareLinkResponse } from '../../core/api/model/shareLinkResponse';
import { ViewPasswordResponse } from '../../core/api/model/viewPasswordResponse';
import { VaultEntryModalComponent } from './vault-entry-modal/vault-entry-modal.component';
import { MasterPasswordModalComponent } from './master-password-modal/master-password-modal.component';
import { ShareModalComponent } from '../secure-sharing/share-modal/share-modal.component';
import { ShareCreatedModalComponent } from '../secure-sharing/share-created-modal/share-created-modal.component';
import { NotificationEventService } from '../../core/services/notification-event.service';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-vault',
  standalone: true,
  imports: [CommonModule, FormsModule, VaultEntryModalComponent, MasterPasswordModalComponent, ShareModalComponent, ShareCreatedModalComponent, LucideAngularModule],
  templateUrl: './vault.component.html',
  styleUrl: './vault.component.css'
})
export class VaultComponent implements OnInit {
  private vaultService = inject(VaultService);
  private categoryService = inject(CategoryControllerService);
  private folderService = inject(FolderControllerService);
  private notificationEventService = inject(NotificationEventService);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  entries: VaultEntryResponse[] = [];
  filteredEntries: VaultEntryResponse[] = [];
  isLoading = true;
  errorMessage = '';
  searchQuery = '';
  currentFilter: { type: 'system' | 'category' | 'folder', id?: number, name: string } = { type: 'system', id: undefined, name: 'all' };

  // ── Advanced Search Filters ────────────────────────────────────────────────
  showFilters = false;
  categories: CategoryDTO[] = [];
  folders: FolderDTO[] = [];
  selectedCategoryId?: number;
  selectedFolderId?: number;
  filterFavorites = false;
  filterHighlySensitive = false;
  sortBy = 'title';
  sortDir: 'asc' | 'desc' = 'asc';

  showModal = false;
  selectedEntry: VaultEntryDetailResponse | null = null;
  /** Patched after the user unlocks a sensitive entry for editing */
  unlockedEntry: VaultEntryDetailResponse | null = null;

  // ── Trash view ─────────────────────────────────────────────────────────────
  isTrashView = false;
  trashEntries: TrashEntryResponse[] = [];
  isLoadingTrash = false;

  // ── Copy-password flow (existing) ──────────────────────────────────────────
  showMasterPasswordModal = false;
  entryToDecrypt: number | null = null;
  decryptedPasswordTimeout: any;
  copiedEntryId: number | null = null;

  // ── Sensitive-unlock-for-edit flow ─────────────────────────────────────────
  showSensitiveUnlockModal = false;
  sensitiveUnlockEntryId: number | null = null;

  // ── Password History ───────────────────────────────────────────────────────
  showHistoryModal = false;
  historyEntryTitle = '';
  passwordHistory: SnapshotResponse[] = [];
  isLoadingHistory = false;

  // ── Secure Sharing ─────────────────────────────────────────────────────────
  showShareModal = false;
  showShareCreatedModal = false;
  selectedEntryForShare: VaultEntryResponse | null = null;
  createdShare: ShareLinkResponse | null = null;

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      // Clear detail view when navigating
      this.closeModal();

      const filter = params['filter'];
      const categoryId = params['categoryId'];

      if (filter === 'trash') {
        this.isTrashView = true;
        this.currentFilter = { type: 'system', name: 'trash' };
        if (this.trashEntries.length === 0) {
          this.loadTrashEntries();
        }
      } else if (filter === 'recent') {
        this.isTrashView = false;
        this.currentFilter = { type: 'system', name: 'recent' };
        this.loadRecentEntries();
      } else {
        this.isTrashView = false;
        if (filter === 'favorites') {
          this.currentFilter = { type: 'system', name: 'favorites' };
        } else if (categoryId) {
          this.currentFilter = { type: 'category', id: +categoryId, name: 'category' };
        } else {
          this.currentFilter = { type: 'system', name: 'all' };
        }

        // If we haven't loaded main entries yet, load them, otherwise apply filter directly
        if (this.entries.length === 0) {
          this.loadEntries();
        } else {
          this.applyFilter();
        }
      }

      const openEntryId = params['openEntryId'];
      if (openEntryId) {
        this.vaultService.getEntry(Number(openEntryId)).subscribe({
          next: (detailEntry) => {
            this.selectedEntry = detailEntry;
            this.unlockedEntry = null;
            this.showModal = true;
          },
          error: () => {
            this.errorMessage = 'Failed to load specified entry from dashboard.';
          }
        });
      }
    });
  }

  loadEntries() {
    this.isLoading = true;
    this.errorMessage = '';
    this.vaultService.getAllEntries().subscribe({
      next: (data) => {
        // Filter out entries without valid IDs or titles to prevent rendering issues
        const validEntries = (data || []).filter(entry => entry && entry.id != null && entry.title && entry.title.trim() !== '');
        this.entries = [...validEntries];
        this.applyFilter();
        this.isLoading = false;
        // Trigger change detection in next cycle
        setTimeout(() => this.cdr.markForCheck(), 0);
      },
      error: (err) => {
        // In duress mode, show empty vault without error
        if (localStorage.getItem('duress_mode') === 'true') {
          this.entries = [];
          this.applyFilter();
        } else {
          this.errorMessage = 'Failed to load vault entries.';
        }
        this.isLoading = false;
      }
    });
  }

  loadTrashEntries() {
    this.isLoadingTrash = true;
    this.errorMessage = '';
    this.vaultService.getTrashEntries().subscribe({
      next: (data) => {
        this.trashEntries = data;
        this.isLoadingTrash = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load trash entries.';
        this.isLoadingTrash = false;
      }
    });
  }

  loadRecentEntries() {
    this.isLoading = true;
    this.errorMessage = '';
    this.vaultService.getRecentEntries().subscribe({
      next: (data) => {
        // Filter out entries without valid IDs or titles to prevent rendering issues
        const validEntries = (data || []).filter(entry => entry && entry.id != null && entry.title && entry.title.trim() !== '');
        this.entries = [...validEntries];
        this.filteredEntries = [...validEntries];
        this.isLoading = false;
        setTimeout(() => this.cdr.markForCheck(), 0);
      },
      error: () => {
        this.errorMessage = 'Failed to load recent entries.';
        this.isLoading = false;
      }
    });
  }

  private applyFilter() {
    let filtered = this.entries;

    // 1. Sidebar Filter
    if (this.currentFilter.type === 'system') {
      if (this.currentFilter.name === 'favorites') {
        filtered = filtered.filter(e => e.isFavorite);
      }
      // 'trash' is handled by isTrashView + trashEntries
    } else if (this.currentFilter.type === 'category') {
      filtered = filtered.filter(e => e.categoryId === this.currentFilter.id);
    } else if (this.currentFilter.type === 'folder') {
      filtered = filtered.filter(e => (e as any).folderId === this.currentFilter.id);
    }

    // 2. Search Query Filter
    if (this.searchQuery && this.searchQuery.trim().length > 0) {
      const q = this.searchQuery.toLowerCase().trim();
      filtered = filtered.filter(e =>
        (e.title && e.title.toLowerCase().includes(q)) ||
        (e.username && e.username.toLowerCase().includes(q)) ||
        (e.websiteUrl && e.websiteUrl.toLowerCase().includes(q))
      );
    }

    this.filteredEntries = [...filtered]; // Create new array reference for change detection
  }

  onSearch() {
    // If any advanced filters are active, use API search
    if (this.hasActiveFilters()) {
      this.performAdvancedSearch();
    } else {
      this.applyFilter();
    }
  }

  hasActiveFilters(): boolean {
    return !!this.selectedCategoryId ||
      !!this.selectedFolderId ||
      this.filterFavorites ||
      this.filterHighlySensitive ||
      this.sortBy !== 'title' ||
      this.sortDir !== 'asc';
  }

  toggleFilters() {
    this.showFilters = !this.showFilters;
    if (this.showFilters && this.categories.length === 0) {
      this.loadCategories();
      this.loadFolders();
    }
  }

  loadCategories() {
    this.categoryService.getAllCategories().subscribe({
      next: (cats) => { this.categories = cats; },
      error: () => { /* silently fail */ }
    });
  }

  loadFolders() {
    this.folderService.getFolders().subscribe({
      next: (folds: FolderDTO[]) => { this.folders = folds; },
      error: () => { /* silently fail */ }
    });
  }

  performAdvancedSearch() {
    this.isLoading = true;
    this.vaultService.searchEntries(
      this.searchQuery.trim() || undefined,
      this.selectedCategoryId,
      this.selectedFolderId,
      this.filterFavorites || undefined,
      this.filterHighlySensitive || undefined,
      this.sortBy,
      this.sortDir
    ).subscribe({
      next: (data) => {
        this.filteredEntries = data;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to search entries.';
        this.isLoading = false;
      }
    });
  }

  clearFilters() {
    this.selectedCategoryId = undefined;
    this.selectedFolderId = undefined;
    this.filterFavorites = false;
    this.filterHighlySensitive = false;
    this.sortBy = 'title';
    this.sortDir = 'asc';
    this.searchQuery = '';
    this.showFilters = false;
    this.loadEntries();
  }

  onAddNew() {
    this.selectedEntry = null;
    this.unlockedEntry = null;
    this.showModal = true;
  }

  onViewEntry(entry: VaultEntryResponse) {
    if (!entry.id) {
      this.selectedEntry = entry as unknown as VaultEntryDetailResponse;
      this.unlockedEntry = null;
      this.showModal = true;
      return;
    }
    this.vaultService.getEntry(entry.id).subscribe({
      next: (detailEntry) => {
        console.log('Fetched Detail Entry from Backend:', detailEntry);
        this.selectedEntry = detailEntry;
        this.unlockedEntry = null;
        this.showModal = true;
      },
      error: () => {
        this.errorMessage = 'Failed to load entry details.';
      }
    });
  }

  closeModal() {
    this.showModal = false;
    this.selectedEntry = null;
    this.unlockedEntry = null;
  }

  saveEntry(request: VaultEntryRequest) {
    const requestObservable = this.selectedEntry?.id
      ? this.vaultService.updateEntry(this.selectedEntry.id, request)
      : this.vaultService.createEntry(request);

    requestObservable.subscribe({
      next: () => {
        this.closeModal();
        this.loadEntries();
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        console.error('Failed to save the vault entry:', err.status, err.message, err.error);
        let detailedError = 'Failed to save the vault entry.';
        if (err.error && typeof err.error === 'string') {
          detailedError = err.error;
        } else if (err.error && err.error.message) {
          detailedError = err.error.message;
        }
        alert('DEBUG: HTTP Status: ' + err.status + ' message: ' + detailedError);
        this.errorMessage = detailedError;
        this.closeModal();
      }
    });
  }

  toggleFavorite(event: Event, entry: VaultEntryResponse) {
    event.stopPropagation();
    if (!entry.id) return;
    this.vaultService.toggleFavorite(entry.id).subscribe({
      next: () => {
        this.loadEntries();
        this.notificationEventService.triggerRefresh();
      },
      error: () => { this.errorMessage = 'Failed to toggle favorite status.'; }
    });
  }

  deleteEntry(event: Event, entry: VaultEntryResponse) {
    event.stopPropagation();
    if (!entry.id) return;
    if (confirm(`Are you sure you want to move "${entry.title}" to the trash?`)) {
      this.vaultService.deleteEntry(entry.id).subscribe({
        next: () => {
          this.loadEntries();
          this.notificationEventService.triggerRefresh();
        },
        error: () => { this.errorMessage = 'Failed to move entry to trash.'; }
      });
    }
  }

  // ── Trash actions ───────────────────────────────────────────────────────────

  restoreEntry(event: Event, entry: TrashEntryResponse) {
    event.stopPropagation();
    if (!entry.id) return;
    this.vaultService.restoreEntry(entry.id).subscribe({
      next: () => { this.loadTrashEntries(); },
      error: () => { this.errorMessage = 'Failed to restore entry.'; }
    });
  }

  permanentDeleteEntry(event: Event, entry: TrashEntryResponse) {
    event.stopPropagation();
    if (!entry.id) return;
    if (confirm(`Permanently delete "${entry.title}"? This cannot be undone.`)) {
      this.vaultService.permanentDelete(entry.id).subscribe({
        next: () => {
          this.loadTrashEntries();
          this.notificationEventService.triggerRefresh();
        },
        error: () => { this.errorMessage = 'Failed to permanently delete entry.'; }
      });
    }
  }

  /**
   * Fallback when a favicon image fails to load — hide the image to show initial underneath.
   */
  onFaviconError(event: Event, title: string | undefined) {
    const img = event.target as HTMLImageElement;
    // Hide the broken image to reveal the initial underneath
    img.style.display = 'none';
  }

  emptyTrash() {
    if (confirm('Empty trash? All items will be permanently deleted.')) {
      this.vaultService.emptyTrash().subscribe({
        next: () => {
          this.loadTrashEntries();
          this.notificationEventService.triggerRefresh();
        },
        error: () => { this.errorMessage = 'Failed to empty trash.'; }
      });
    }
  }

  restoreAllTrash() {
    if (this.trashEntries.length === 0) return;
    if (confirm(`Restore all ${this.trashEntries.length} items from trash?`)) {
      this.vaultService.restoreAll().subscribe({
        next: () => {
          this.loadTrashEntries();
          this.notificationEventService.triggerRefresh();
        },
        error: () => { this.errorMessage = 'Failed to restore items from trash.'; }
      });
    }
  }

  // ── Password History ───────────────────────────────────────────────────────

  viewPasswordHistory(event: Event, entry: VaultEntryResponse) {
    event.stopPropagation();
    if (!entry.id) return;
    this.historyEntryTitle = entry.title || 'Entry';
    this.showHistoryModal = true;
    this.isLoadingHistory = true;
    this.passwordHistory = [];

    this.vaultService.getPasswordHistory(entry.id).subscribe({
      next: (history) => {
        this.passwordHistory = history;
        this.isLoadingHistory = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load password history.';
        this.isLoadingHistory = false;
      }
    });
  }

  closeHistoryModal() {
    this.showHistoryModal = false;
    this.passwordHistory = [];
    this.historyEntryTitle = '';
  }

  // ── Copy-password flow (existing) ──────────────────────────────────────────

  requestPasswordCopy(event: Event, entry: VaultEntryResponse) {
    event.stopPropagation();
    if (!entry.id) return;

    // Feature: Highly sensitive passwords require master password confirmation
    // Note: VaultEntryResponse might not have `isHighlySensitive` directly mapped if it wasn't added to the DTO yet, 
    // but the backend's `/view-password` endpoint will fail if it's sensitive and the OTP wasn't provided.
    // However, the cleanest way to do this from the list view is to see if we can get the password directly first.

    // We try to view the password directly.
    this.vaultService.viewPassword(entry.id).subscribe({
      next: (response: ViewPasswordResponse) => {
        if (response.password) {
          navigator.clipboard.writeText(response.password).then(() => {
            this.copiedEntryId = entry.id!;
            if (this.decryptedPasswordTimeout) clearTimeout(this.decryptedPasswordTimeout);
            this.decryptedPasswordTimeout = setTimeout(() => {
              this.copiedEntryId = null;
            }, 2000);
          }).catch(err => {
            console.error('Failed to copy password:', err);
            this.errorMessage = 'Failed to copy password to clipboard.';
          });
        }
      },
      error: (err) => {
        // If it's 403 or 401, it probably means it's highly sensitive and needs the master password/OTP flow
        if (err.status === 403 || err.status === 401 || err.status === 400 /* some backends return 400 for structural auth fails */) {
          this.entryToDecrypt = entry.id!;
          this.showMasterPasswordModal = true;
        } else {
          this.errorMessage = 'Failed to retrieve password for copying.';
        }
      }
    });
  }

  handlePasswordDecrypted(password: string) {
    this.showMasterPasswordModal = false;
    navigator.clipboard.writeText(password).then(() => {
      if (this.entryToDecrypt) {
        this.copiedEntryId = this.entryToDecrypt;
        if (this.decryptedPasswordTimeout) clearTimeout(this.decryptedPasswordTimeout);
        this.decryptedPasswordTimeout = setTimeout(() => {
          this.copiedEntryId = null;
          this.entryToDecrypt = null;
        }, 2000);
      }
    }).catch(err => {
      console.error('Failed to copy password:', err);
      this.errorMessage = 'Failed to copy password to clipboard.';
    });
  }

  closeMasterPasswordModal() {
    this.showMasterPasswordModal = false;
    this.entryToDecrypt = null;
  }

  // ── Sensitive-unlock-for-edit flow ─────────────────────────────────────────

  onSensitiveUnlockRequested(entryId: number) {
    this.sensitiveUnlockEntryId = entryId;
    this.showSensitiveUnlockModal = true;
  }

  onEntryUnlocked(detail: VaultEntryDetailResponse) {
    this.showSensitiveUnlockModal = false;
    this.sensitiveUnlockEntryId = null;
    this.unlockedEntry = detail;
  }

  closeSensitiveUnlockModal() {
    this.showSensitiveUnlockModal = false;
    this.sensitiveUnlockEntryId = null;
  }

  // ── Secure Sharing Handlers ─────────────────────────────────────────────────

  onShareRequested(entryId: number): void {
    // Find the entry in the list to get the title
    this.selectedEntryForShare = this.entries.find(e => e.id === entryId) || null;
    this.showShareModal = true;
  }

  onShareCreated(share: ShareLinkResponse): void {
    this.createdShare = share;
    this.showShareModal = false;
    this.showShareCreatedModal = true;
  }

  onShareCancelled(): void {
    this.showShareModal = false;
    this.selectedEntryForShare = null;
  }

  onShareCreatedClosed(): void {
    this.showShareCreatedModal = false;
    this.createdShare = null;
    this.selectedEntryForShare = null;
  }

  onCreateAnotherShare(): void {
    this.showShareCreatedModal = false;
    this.createdShare = null;
    this.showShareModal = true;
  }
}
