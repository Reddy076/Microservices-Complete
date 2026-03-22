import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VaultComponent } from './vault.component';
import { VaultService } from '../../core/api/api/vault.service';
import { of, throwError } from 'rxjs';
import { Component, Input, Output, EventEmitter, importProvidersFrom } from '@angular/core';
import { VaultEntryResponse } from '../../core/api/model/vaultEntryResponse';
import { VaultEntryDetailResponse } from '../../core/api/model/vaultEntryDetailResponse';
import { VaultEntryModalComponent } from './vault-entry-modal/vault-entry-modal.component';
import { MasterPasswordModalComponent } from './master-password-modal/master-password-modal.component';
import { CategorySidebarComponent } from './category-sidebar/category-sidebar.component';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import {
  LucideAngularModule, Search, Plus, Eye, EyeOff, Star, Trash2, Edit2, Copy,
  Lock, RotateCw, X, Check, Shield, Folder, ArrowRight, Key, Sliders, Clock
} from 'lucide-angular';

// Mock child components
@Component({ selector: 'app-vault-entry-modal', standalone: true, template: '' })
class MockVaultEntryModalComponent {
  @Input() entryToEdit: any;
  @Output() save = new EventEmitter<any>();
  @Output() cancel = new EventEmitter<void>();
}

@Component({ selector: 'app-master-password-modal', standalone: true, template: '' })
class MockMasterPasswordModalComponent {
  @Input() entryId: number | null = null;
  @Output() passwordDecrypted = new EventEmitter<string>();
  @Output() modalCancel = new EventEmitter<void>();
}

@Component({ selector: 'app-category-sidebar', standalone: true, template: '' })
class MockCategorySidebarComponent {
  @Output() filterSelected = new EventEmitter<any>();
}

describe('VaultComponent', () => {
  let component: VaultComponent;
  let fixture: ComponentFixture<VaultComponent>;
  let mockVaultService: jasmine.SpyObj<VaultService>;

  const mockEntries: VaultEntryResponse[] = [
    { id: 1, title: 'Netflix', isFavorite: true, isHighlySensitive: false } as any,
    { id: 2, title: 'Bank', isFavorite: false, isHighlySensitive: true } as any
  ];

  beforeEach(async () => {
    mockVaultService = jasmine.createSpyObj('VaultService', ['getAllEntries', 'getEntry', 'toggleFavorite', 'deleteEntry', 'viewPassword']);
    mockVaultService.getAllEntries.and.returnValue(of(mockEntries) as any);
    mockVaultService.viewPassword.and.returnValue(throwError(() => ({ status: 403 })));

    await TestBed.configureTestingModule({
      imports: [VaultComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        importProvidersFrom(LucideAngularModule.pick({
          Search, Plus, Eye, EyeOff, Star, Trash2, Edit2, Copy, Lock, RotateCw, X, Check, Shield, Folder, ArrowRight, Key, Sliders, Clock
        })),
        { provide: VaultService, useValue: mockVaultService }
      ]
    })
      .overrideComponent(VaultComponent, {
        remove: { imports: [VaultEntryModalComponent, MasterPasswordModalComponent, CategorySidebarComponent] },
        add: { imports: [MockVaultEntryModalComponent, MockMasterPasswordModalComponent, MockCategorySidebarComponent] }
      })
      .compileComponents();

    fixture = TestBed.createComponent(VaultComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load entries on init', () => {
    expect(component).toBeTruthy();
    expect(mockVaultService.getAllEntries).toHaveBeenCalled();
    expect(component.entries.length).toBe(2);
    expect(component.filteredEntries.length).toBe(2);
    expect(component.isLoading).toBeFalse();
  });

  it('should filter entries based on sidebar selection', () => {
    // Both entries don't have isDeleted set, so isDeleted is effectively false/undefined
    (component as any).currentFilter = { type: 'system', name: 'favorites' };
    (component as any).applyFilter();
    expect(component.filteredEntries.length).toBe(1);
    expect(component.filteredEntries[0].title).toBe('Netflix');
  });

  it('should open new entry modal', () => {
    component.onAddNew();
    expect(component.showModal).toBeTrue();
    expect(component.selectedEntry).toBeNull();
  });

  it('should toggle favorite status', () => {
    mockVaultService.toggleFavorite.and.returnValue(of({}) as any);
    const mockEvent = new Event('click');
    spyOn(mockEvent, 'stopPropagation');

    component.toggleFavorite(mockEvent, mockEntries[0]);
    expect(mockEvent.stopPropagation).toHaveBeenCalled();
    expect(mockVaultService.toggleFavorite).toHaveBeenCalledWith(1);
    expect(mockVaultService.getAllEntries).toHaveBeenCalledTimes(2); // once on init, once on toggle
  });

  it('should initiate master password decrypt flow', () => {
    const mockEvent = new Event('click');
    spyOn(mockEvent, 'stopPropagation');

    component.requestPasswordCopy(mockEvent, mockEntries[1]);
    expect(mockEvent.stopPropagation).toHaveBeenCalled();
    expect(component.entryToDecrypt).toBe(2);
    expect(component.showMasterPasswordModal).toBeTrue();
  });
});
