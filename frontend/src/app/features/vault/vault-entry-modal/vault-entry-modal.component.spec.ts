import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VaultEntryModalComponent } from './vault-entry-modal.component';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { CategoryControllerService } from '../../../core/api/api/categoryController.service';
import { FolderControllerService } from '../../../core/api/api/folderController.service';
import { PasswordGeneratorWidgetComponent } from '../password-generator-widget/password-generator-widget.component';
import { of } from 'rxjs';
import { importProvidersFrom } from '@angular/core';
import { LucideAngularModule, Plus, X, Eye, EyeOff, RefreshCw, Copy, Globe, Star, Shield, Lock, Trash2, Key, Edit2, RotateCw, FolderOpen, Folder, ArrowRight, Settings2, AlertTriangle, FileText, File, ChevronDown } from 'lucide-angular';

describe('VaultEntryModalComponent', () => {
  let component: VaultEntryModalComponent;
  let fixture: ComponentFixture<VaultEntryModalComponent>;
  let mockCategoryService: jasmine.SpyObj<CategoryControllerService>;
  let mockFolderService: jasmine.SpyObj<FolderControllerService>;

  beforeEach(async () => {
    mockCategoryService = jasmine.createSpyObj('CategoryControllerService', ['getAllCategories']);
    mockFolderService = jasmine.createSpyObj('FolderControllerService', ['getFolders']);

    mockCategoryService.getAllCategories.and.returnValue(of([{ id: 1, name: 'Work' }]) as any);
    mockFolderService.getFolders.and.returnValue(of([{ id: 10, name: 'Taxes' }]) as any);

    // Mocking the child component so it doesn't try to inject its own services
    await TestBed.configureTestingModule({
      imports: [VaultEntryModalComponent, ReactiveFormsModule],
      providers: [
        FormBuilder,
        importProvidersFrom(LucideAngularModule.pick({ Plus, X, Eye, EyeOff, RefreshCw, Copy, Globe, Star, Shield, Lock, Trash2, Key, Edit2, RotateCw, Folder, ArrowRight, FolderOpen, Settings2, AlertTriangle, FileText, File, ChevronDown })),
        { provide: CategoryControllerService, useValue: mockCategoryService },
        { provide: FolderControllerService, useValue: mockFolderService }
      ]
    })
      // Override the standalone component to swap out the real PasswordGeneratorWidgetComponent with a mock? 
      // Or just provide its dependencies. It's easier to provide its dependencies at root level if it's imported.
      .overrideComponent(VaultEntryModalComponent, {
        remove: { imports: [PasswordGeneratorWidgetComponent] },
        add: { imports: [] }
      })
      .compileComponents();

    fixture = TestBed.createComponent(VaultEntryModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form on init', () => {
    expect(component.entryForm).toBeDefined();
    expect(component.entryForm.get('title')).toBeDefined();
    expect(component.entryForm.get('categoryId')).toBeDefined();
  });

  it('should load categories and folders on init', () => {
    expect(mockCategoryService.getAllCategories).toHaveBeenCalled();
    expect(mockFolderService.getFolders).toHaveBeenCalled();
    expect(component.categories.length).toBe(1);
    expect(component.folders.length).toBe(1);
  });

  it('should correctly handle password generation event', () => {
    component.onPasswordGenerated('NewPass123!');
    expect(component.entryForm.value.password).toBe('NewPass123!');
    expect(component.showGenerator).toBeFalse();
  });

  it('should emit save event when form is valid and submitted', () => {
    spyOn(component.save, 'emit');

    component.entryForm.patchValue({
      title: 'Valid Entry',
      username: 'user@example.com',
      password: 'password123',
      categoryId: '1',
      folderId: '10',
      isFavorite: true
    });

    component.onSubmit();

    expect(component.save.emit).toHaveBeenCalledWith(jasmine.objectContaining({
      title: 'Valid Entry',
      username: 'user@example.com',
      categoryId: 1,
      folderId: 10,
      isFavorite: true
    }));
  });

  it('should emit cancel event on cancel', () => {
    spyOn(component.cancel, 'emit');
    component.onCancel();
    expect(component.cancel.emit).toHaveBeenCalled();
  });
});
