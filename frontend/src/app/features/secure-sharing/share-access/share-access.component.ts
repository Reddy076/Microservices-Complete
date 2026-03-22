import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { SecureSharingService } from '../../../core/api/api/secureSharing.service';
import { SharedPasswordResponse } from '../../../core/api/model/sharedPasswordResponse';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-share-access',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './share-access.component.html',
  styleUrl: './share-access.component.css'
})
export class ShareAccessComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private sharingService = inject(SecureSharingService);

  isLoading = true;
  error: string | null = null;
  decryptedPassword = '';
  passwordVisible = false;

  sharedData: SharedPasswordResponse | null = null;
  copySuccess = false;

  async ngOnInit() {
    const token = this.route.snapshot.paramMap.get('token');
    // Read the fragment hash (minus the #)
    let keyStr = '';
    // if using fragment
    if (typeof window !== 'undefined' && window.location.hash) {
      keyStr = window.location.hash.substring(1);
    }

    if (!token || !keyStr) {
      this.error = 'Invalid secure link. This might happen if you did not copy the complete URL (including the #part at the end).';
      this.isLoading = false;
      return;
    }

    try {
      this.sharingService.getSharedPassword(token).subscribe({
        next: async (res) => {
          this.sharedData = res;
          try {
            this.decryptedPassword = await this.decryptPassword(
              res.encryptedPassword!,
              res.encryptionIv!,
              keyStr
            );
            this.isLoading = false;
          } catch (err) {
            console.error('Decryption failed:', err);
            this.error = 'Failed to decrypt the password. The encryption key in the URL seems to be invalid or corrupted.';
            this.isLoading = false;
          }
        },
        error: (err) => {
          this.error = err.error?.message || 'This secure link is invalid, has expired, or the view limit has been reached.';
          this.isLoading = false;
        }
      });
    } catch (err) {
      this.error = 'An unexpected error occurred while communicating with the server.';
      this.isLoading = false;
    }
  }

  // Helper: decrypt AES-GCM
  private async decryptPassword(ciphertextBase64: string, ivBase64: string, keyBase64: string): Promise<string> {
    const keyBytes = this.base64ToArrayBuffer(keyBase64) as unknown as BufferSource;
    const cryptoKey = await crypto.subtle.importKey(
      'raw',
      keyBytes,
      { name: 'AES-GCM' },
      false,
      ['decrypt']
    );

    const ivBytes = this.base64ToArrayBuffer(ivBase64) as unknown as BufferSource;
    const ciphertextBytes = this.base64ToArrayBuffer(ciphertextBase64) as unknown as BufferSource;

    const decryptedBuffer = await crypto.subtle.decrypt(
      {
        name: 'AES-GCM',
        iv: ivBytes,
        tagLength: 128
      },
      cryptoKey,
      ciphertextBytes
    );

    const decoder = new TextDecoder();
    return decoder.decode(decryptedBuffer);
  }

  private base64ToArrayBuffer(base64: string): Uint8Array {
    const binaryString = window.atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes;
  }

  togglePasswordVisibility() {
    this.passwordVisible = !this.passwordVisible;
  }

  copyToClipboard() {
    navigator.clipboard.writeText(this.decryptedPassword).then(() => {
      this.copySuccess = true;
      setTimeout(() => this.copySuccess = false, 2000);
    }).catch(err => {
      // Fallback
      const textarea = document.createElement('textarea');
      textarea.value = this.decryptedPassword;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      try {
        document.execCommand('copy');
        this.copySuccess = true;
        setTimeout(() => this.copySuccess = false, 2000);
      } catch (e) {
        console.error('Failed to copy fallback:', e);
      }
      document.body.removeChild(textarea);
    });
  }
}
