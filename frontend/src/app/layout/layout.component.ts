import { Component, inject, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule, NgIf } from '@angular/common';
import { AuthenticationService } from '../core/api';
import { IdleService } from '../core/services/idle.service';
import { TopHeaderComponent } from './top-header/top-header.component';
import { CategoryControllerService } from '../core/api/api/categoryController.service';
import { CategoryDTO } from '../core/api/model/categoryDTO';
import { LucideAngularModule } from 'lucide-angular';
import { ChatbotWidgetComponent } from '../shared/chatbot-widget/chatbot-widget.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, CommonModule, NgIf, TopHeaderComponent,
    LucideAngularModule, ChatbotWidgetComponent
  ],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css'
})
export class LayoutComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthenticationService);
  private readonly idleService = inject(IdleService);
  private readonly categoryService = inject(CategoryControllerService);

  sidebarCollapsed = false;
  isMobileNavOpen = false;
  categories: CategoryDTO[] = [];
  categoriesLoading = false;

  ngOnInit() {
    this.idleService.startWatching();
    this.loadCategories();
  }

  ngOnDestroy() {
    this.idleService.stopWatching();
  }

  loadCategories() {
    this.categoriesLoading = true;
    this.categoryService.getAllCategories().subscribe({
      next: (cats) => {
        this.categories = cats || [];
        this.categoriesLoading = false;
      },
      error: () => {
        this.categoriesLoading = false;
      }
    });
  }

  get hasCategories(): boolean {
    return this.categories && this.categories.length > 0;
  }

  toggleSidebar() {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  toggleMobileNav() {
    this.isMobileNavOpen = !this.isMobileNavOpen;
  }

  closeMobileNav() {
    this.isMobileNavOpen = false;
  }

  @HostListener('window:resize')
  onResize() {
    if (window.innerWidth > 768) {
      this.isMobileNavOpen = false;
    }
  }

  logout() {
    this.isMobileNavOpen = false;
    this.idleService.stopWatching();
    const token = localStorage.getItem('access_token');

    const finalizeLogout = () => {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      this.router.navigate(['/login']);
    };

    if (token) {
      this.authService.logout(`Bearer ${token}`).subscribe({
        next: () => finalizeLogout(),
        error: () => finalizeLogout()
      });
    } else {
      finalizeLogout();
    }
  }

  get currentUsername(): string {
    try {
      const token = localStorage.getItem('access_token');
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.sub || 'User';
      }
    } catch {
      // ignore parse errors
    }
    return 'User';
  }
}
