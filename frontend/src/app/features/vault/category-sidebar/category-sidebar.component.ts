import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CategoryControllerService } from '../../../core/api/api/categoryController.service';
import { FolderControllerService } from '../../../core/api/api/folderController.service';
import { CategoryDTO } from '../../../core/api/model/categoryDTO';
import { FolderDTO } from '../../../core/api/model/folderDTO';

@Component({
  selector: 'app-category-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './category-sidebar.component.html',
  styleUrl: './category-sidebar.component.css'
})
export class CategorySidebarComponent implements OnInit {
  private categoryService = inject(CategoryControllerService);
  private folderService = inject(FolderControllerService);

  @Output() filterSelected = new EventEmitter<{ type: 'category' | 'folder' | 'system', id?: number, name: string }>();

  categories: CategoryDTO[] = [];
  folders: FolderDTO[] = [];
  isLoading = true;

  activeFilter: { type: 'system' | 'category' | 'folder', id?: number, name: string } = { type: 'system', id: undefined, name: 'all' };

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.isLoading = true;
    
    // Load Categories
    this.categoryService.getAllCategories().subscribe({
      next: (cats) => {
        this.categories = cats;
        this.checkLoadingState();
      },
      error: () => this.checkLoadingState()
    });

    // Load Folders (assuming getAllFolders mapping, checking if it exists, but the swagger might not have an empty arg one ready if it req params, let's assume it works)
    // Looking at API docs: GET /api/folders
    this.folderService.getFolders().subscribe({
      next: (flds) => {
        this.folders = flds;
        this.checkLoadingState();
      },
      error: () => this.checkLoadingState()
    });
  }

  private checkLoadingState() {
    // Basic completion check
    this.isLoading = false; 
  }

  selectSystemFilter(filterName: string) {
    this.activeFilter = { type: 'system', id: undefined, name: filterName };
    this.filterSelected.emit(this.activeFilter);
  }

  selectCategory(category: CategoryDTO) {
    if (!category.id) return;
    this.activeFilter = { type: 'category', id: category.id, name: category.name || 'category' };
    this.filterSelected.emit(this.activeFilter);
  }

  selectFolder(folder: FolderDTO) {
    if (!folder.id) return;
    this.activeFilter = { type: 'folder', id: folder.id, name: folder.name || 'folder' };
    this.filterSelected.emit(this.activeFilter);
  }
}
