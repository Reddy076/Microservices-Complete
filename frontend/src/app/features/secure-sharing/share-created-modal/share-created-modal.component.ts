import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { QRCodeModule } from 'angularx-qrcode';
import { ShareLinkResponse } from '../../../core/api/model/shareLinkResponse';

@Component({
  selector: 'app-share-created-modal',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, QRCodeModule],
  templateUrl: './share-created-modal.component.html',
  styleUrl: './share-created-modal.component.css'
})
export class ShareCreatedModalComponent {
  @Input() shareResponse!: ShareLinkResponse;

  @Output() closed = new EventEmitter<void>();
  @Output() createAnother = new EventEmitter<void>();

  copyStates = {
    fullLink: false
  };

  get fullShareLink(): string {
    if (!this.shareResponse || typeof window === 'undefined') return '';
    return `${window.location.origin}/share/${this.shareResponse.shareToken}#${this.shareResponse.encryptionKey}`;
  }

  get formattedExpiry(): string {
    if (!this.shareResponse?.expiresAt) return '';
    const expiryDate = new Date(this.shareResponse.expiresAt);
    const now = new Date();
    const diffMs = expiryDate.getTime() - now.getTime();
    const diffHours = Math.ceil(diffMs / (1000 * 60 * 60));

    if (diffHours < 24) {
      return `${diffHours} hour${diffHours > 1 ? 's' : ''}`;
    } else {
      const days = Math.floor(diffHours / 24);
      const hours = diffHours % 24;
      if (hours === 0) {
        return `${days} day${days > 1 ? 's' : ''}`;
      }
      return `${days}d ${hours}h`;
    }
  }

  get formattedMaxViews(): string {
    if (!this.shareResponse) return '';
    if (this.shareResponse.maxViews && this.shareResponse.maxViews >= 100) {
      return 'Unlimited';
    }
    return `${this.shareResponse.maxViews} view${this.shareResponse.maxViews && this.shareResponse.maxViews > 1 ? 's' : ''}`;
  }

  async copyToClipboard(): Promise<void> {
    const textToCopy = this.fullShareLink;

    try {
      await navigator.clipboard.writeText(textToCopy);
      this.showCopySuccess();
    } catch (err) {
      // Fallback for older browsers
      const textarea = document.createElement('textarea');
      textarea.value = textToCopy;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      try {
        document.execCommand('copy');
        this.showCopySuccess();
      } catch (e) {
        console.error('Failed to copy:', e);
      }
      document.body.removeChild(textarea);
    }
  }

  private showCopySuccess(): void {
    this.copyStates.fullLink = true;
    setTimeout(() => {
      this.copyStates.fullLink = false;
    }, 2000);
  }

  onClose(): void {
    this.closed.emit();
  }

  onCreateAnother(): void {
    this.createAnother.emit();
  }
}
