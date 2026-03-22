import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { LucideAngularModule, MessageCircle, X, Send, Bot, User, Loader2, Sparkles } from 'lucide-angular';
import { VaultService } from '../../core/api/api/vault.service';
import { DashboardApiService } from '../../core/services/dashboard-api.service';
import { ReusedPasswordResponse } from '../../core/models/reused-password-response.model';
import { PasswordHealthMetricsResponse } from '../../core/models/password-health-metrics-response.model';
import { SecurityScoreResponse } from '../../core/models/security-score-response.model';
import { VaultEntryResponse } from '../../core/api/model/vaultEntryResponse';

interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
    timestamp: Date;
}

interface ChatResponse {
    message: string;
}

@Component({
    selector: 'app-chatbot-widget',
    standalone: true,
    imports: [CommonModule, FormsModule, LucideAngularModule],
    templateUrl: './chatbot-widget.component.html',
    styleUrl: './chatbot-widget.component.css'
})
export class ChatbotWidgetComponent {
    readonly MessageCircle = MessageCircle;
    readonly X = X;
    readonly Send = Send;
    readonly Bot = Bot;
    readonly User = User;
    readonly Loader2 = Loader2;
    readonly Sparkles = Sparkles;

    isOpen = false;
    userInput = '';
    messages: ChatMessage[] = [];
    isLoading = false;

    private vaultService = inject(VaultService);
    private dashboardApi = inject(DashboardApiService);

    quickPrompts = [
        'How many vault entries do I have?',
        'Show my weak passwords',
        'List reused passwords',
        'How many favorites do I have?',
        'Show old passwords',
        'What is my security score?'
    ];

    constructor(private http: HttpClient) { }

    toggleChat(): void {
        this.isOpen = !this.isOpen;
    }

    sendMessage(): void {
        const userMsg = this.userInput.trim();
        if (!userMsg || this.isLoading) return;

        this.messages.push({ role: 'user', content: userMsg, timestamp: new Date() });
        this.userInput = '';
        this.isLoading = true;

        if (this.isVaultQuery(userMsg)) {
            this.fetchVaultContext(userMsg).subscribe((context: string) => {
                this.sendToAI(userMsg, context);
            });
        } else {
            this.sendToAI(userMsg, null);
        }
    }

    useQuickPrompt(prompt: string): void {
        this.userInput = prompt;
        this.sendMessage();
    }

    closeChat(): void {
        this.isOpen = false;
    }

    private isVaultQuery(msg: string): boolean {
        const lower = msg.toLowerCase();
        return lower.includes('vault') || lower.includes('password') ||
            lower.includes('favorite') || lower.includes('favourite') ||
            lower.includes('weak') || lower.includes('reused') ||
            lower.includes('old') || lower.includes('entries') ||
            lower.includes('entry') || lower.includes('list') ||
            lower.includes('how many') || lower.includes('count') ||
            lower.includes('security score') || lower.includes('score') ||
            lower.includes('health');
    }

    private fetchVaultContext(msg: string) {
        const lower = msg.toLowerCase();

        // Always fetch core counts
        const requests: Record<string, any> = {
            entries: this.vaultService.getAllEntries().pipe(catchError(() => of([]))),
            favorites: this.vaultService.getFavorites().pipe(catchError(() => of([]))),
        };

        // Fetch detailed data based on query intent — using the SAME APIs as the dashboard
        const needsAll = lower.includes('how many') || lower.includes('count') ||
            lower.includes('all') || lower.includes('score') || lower.includes('health');

        if (lower.includes('weak') || needsAll) {
            // Uses /api/dashboard/passwords/weak — same as dashboard
            requests['weak'] = this.dashboardApi.getWeakPasswordsList().pipe(catchError(() => of([])));
        }
        if (lower.includes('reused') || needsAll) {
            // Uses /api/dashboard/reused-passwords — same as dashboard
            requests['reused'] = this.dashboardApi.getReusedPasswords().pipe(catchError(() => of(null)));
        }
        if (lower.includes('old') || needsAll) {
            // Uses /api/dashboard/passwords/old — same as dashboard
            requests['old'] = this.dashboardApi.getOldPasswordsList().pipe(catchError(() => of([])));
        }
        if (lower.includes('score') || lower.includes('health') || needsAll) {
            requests['score'] = this.dashboardApi.getSecurityScore().pipe(catchError(() => of(null)));
            requests['health'] = this.dashboardApi.getPasswordHealth().pipe(catchError(() => of(null)));
        }

        return forkJoin(requests).pipe(
            map((data: Record<string, any>) => this.buildContextString(data)),
            catchError(() => of(''))
        );
    }

    private buildContextString(data: Record<string, any>): string {
        const lines: string[] = ['[USER VAULT DATA]'];

        const entries: VaultEntryResponse[] = data['entries'] || [];
        lines.push(`Total vault entries: ${entries.length}`);
        if (entries.length > 0 && entries.length <= 30) {
            lines.push(`Entry names: ${entries.map(e => e.title || 'Untitled').join(', ')}`);
        } else if (entries.length > 30) {
            lines.push(`Sample entries: ${entries.slice(0, 20).map(e => e.title || 'Untitled').join(', ')}`);
        }

        const favorites: VaultEntryResponse[] = data['favorites'] || [];
        lines.push(`Favorite entries: ${favorites.length}`);
        if (favorites.length > 0) {
            lines.push(`Favorites: ${favorites.slice(0, 10).map(f => f.title || 'Untitled').join(', ')}`);
        }

        if (data['weak']) {
            const weak: VaultEntryResponse[] = data['weak'] || [];
            lines.push(`Weak passwords: ${weak.length}`);
            if (weak.length > 0) {
                lines.push(`Weak password entries: ${weak.slice(0, 10).map(w => w.title || 'Untitled').join(', ')}`);
            }
        }

        if (data['reused']) {
            // ReusedPasswordResponse: { totalReusedGroups, totalAffectedEntries, reusedGroups[] }
            const reused: ReusedPasswordResponse = data['reused'];
            lines.push(`Reused passwords: affects ${reused.totalAffectedEntries} entries across ${reused.totalReusedGroups} groups`);
            if (reused.reusedGroups && reused.reusedGroups.length > 0) {
                reused.reusedGroups.forEach(group => {
                    const names = group.entries.slice(0, 5).map(e => e.title).join(', ');
                    lines.push(`  - Reused group (${group.reuseCount} accounts): ${names}`);
                });
            }
        }

        if (data['old']) {
            const old: VaultEntryResponse[] = data['old'] || [];
            lines.push(`Old passwords (90+ days): ${old.length}`);
            if (old.length > 0) {
                lines.push(`Old password entries: ${old.slice(0, 10).map(o => o.title || 'Untitled').join(', ')}`);
            }
        }

        if (data['score']) {
            const score: SecurityScoreResponse = data['score'];
            lines.push(`Security score: ${score.overallScore ?? 'N/A'}/100 (${score.scoreLabel ?? ''})`);
        }

        if (data['health']) {
            const health: PasswordHealthMetricsResponse = data['health'];
            lines.push(`Password health — Strong: ${health.strongCount}, Good: ${health.goodCount}, Fair: ${health.fairCount}, Weak: ${health.weakCount}, Very Weak: ${health.veryWeakCount}`);
        }

        return lines.join('\n');
    }

    private sendToAI(userMsg: string, vaultContext: string | null): void {
        const messageWithContext = vaultContext
            ? `${vaultContext}\n\nUser question: ${userMsg}`
            : userMsg;

        this.http.post<ChatResponse>(`${environment.apiBaseUrl}/api/ai/chat`, {
            message: messageWithContext
        }).subscribe({
            next: (result) => {
                this.messages.push({
                    role: 'assistant',
                    content: result.message,
                    timestamp: new Date()
                });
                this.isLoading = false;
            },
            error: () => {
                this.messages.push({
                    role: 'assistant',
                    content: 'Sorry, I encountered an error. Please try again.',
                    timestamp: new Date()
                });
                this.isLoading = false;
            }
        });
    }
}
