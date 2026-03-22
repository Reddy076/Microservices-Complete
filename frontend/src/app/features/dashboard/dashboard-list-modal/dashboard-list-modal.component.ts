import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { VaultEntryResponse } from '../../../core/api/model/vaultEntryResponse';

@Component({
  selector: 'app-dashboard-list-modal',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './dashboard-list-modal.component.html',
  styleUrl: './dashboard-list-modal.component.css'
})
export class DashboardListModalComponent {
  @Input() title: string = '';
  @Input() description: string = '';
  @Input() entries: VaultEntryResponse[] = [];
  @Input() isLoading: boolean = false;

  @Output() close = new EventEmitter<void>();
  @Output() entryClick = new EventEmitter<VaultEntryResponse>();

  onClose() {
    this.close.emit();
  }

  onEntryClick(entry: VaultEntryResponse) {
    this.entryClick.emit(entry);
  }

  /**
   * Fallback when a favicon image fails to load — hide the image to show initial underneath.
   */
  onFaviconError(event: Event) {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
  }
}
