import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CategorySidebarComponent } from './category-sidebar.component';
import { CategoryControllerService } from '../../../core/api/api/categoryController.service';
import { FolderControllerService } from '../../../core/api/api/folderController.service';
import { of } from 'rxjs';

describe('CategorySidebarComponent', () => {
  let component: CategorySidebarComponent;
  let fixture: ComponentFixture<CategorySidebarComponent>;
  let mockCategoryService: jasmine.SpyObj<CategoryControllerService>;
  let mockFolderService: jasmine.SpyObj<FolderControllerService>;

  beforeEach(async () => {
    mockCategoryService = jasmine.createSpyObj('CategoryControllerService', ['getAllCategories']);
    mockFolderService = jasmine.createSpyObj('FolderControllerService', ['getFolders']);

    mockCategoryService.getAllCategories.and.returnValue(of([{ id: 1, name: 'Work' }]) as any);
    mockFolderService.getFolders.and.returnValue(of([{ id: 10, name: 'Taxes' }]) as any);

    await TestBed.configureTestingModule({
      imports: [CategorySidebarComponent],
      providers: [
        { provide: CategoryControllerService, useValue: mockCategoryService },
        { provide: FolderControllerService, useValue: mockFolderService }
      ]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CategorySidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load categories and folders on init', () => {
    expect(mockCategoryService.getAllCategories).toHaveBeenCalled();
    expect(mockFolderService.getFolders).toHaveBeenCalled();
    expect(component.categories.length).toBe(1);
    expect(component.folders.length).toBe(1);
    expect(component.isLoading).toBeFalse();
  });

  it('should emit filterSelected when a system filter is selected', () => {
    spyOn(component.filterSelected, 'emit');
    component.selectSystemFilter('favorites');
    expect(component.activeFilter).toEqual({ type: 'system', id: undefined, name: 'favorites' });
    expect(component.filterSelected.emit).toHaveBeenCalledWith({ type: 'system', id: undefined, name: 'favorites' });
  });

  it('should emit filterSelected when a category is selected', () => {
    spyOn(component.filterSelected, 'emit');
    component.selectCategory({ id: 1, name: 'Work' });
    expect(component.activeFilter).toEqual({ type: 'category', id: 1, name: 'Work' });
    expect(component.filterSelected.emit).toHaveBeenCalledWith({ type: 'category', id: 1, name: 'Work' });
  });
});
