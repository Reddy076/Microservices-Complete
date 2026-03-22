import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class NetworkInfoService {
  /**
   * Checks if the IP is a localhost/private address
   */
  isLocalhost(ip: string | null | undefined): boolean {
    if (!ip) return true;

    return ip === '127.0.0.1' ||
      ip === '0:0:0:0:0:0:0:1' ||
      ip === '::1' ||
      ip.startsWith('192.168.') ||
      ip.startsWith('10.') ||
      ip.startsWith('172.');
  }

  /**
   * Returns the display IP string
   * Shows the raw IP address as received from the backend
   */
  getDisplayIp(sessionIp: string | null | undefined): string {
    // Return the raw IP as-is, or 'Unknown' if null
    return sessionIp || 'Unknown';
  }

  /**
   * Returns detailed network info for the session
   */
  getNetworkInfo(ip: string | null | undefined): { type: string; label: string } {
    if (!ip) {
      return { type: 'unknown', label: 'Unknown' };
    }

    if (ip === '127.0.0.1' || ip === '0:0:0:0:0:0:0:1' || ip === '::1') {
      return { type: 'localhost', label: 'This Device' };
    }

    if (ip.startsWith('192.168.')) {
      return { type: 'private', label: 'Home/Office Network' };
    }

    if (ip.startsWith('10.')) {
      return { type: 'private', label: 'Corporate Network' };
    }

    if (ip.startsWith('172.')) {
      return { type: 'private', label: 'Private Network' };
    }

    return { type: 'public', label: ip };
  }
}
