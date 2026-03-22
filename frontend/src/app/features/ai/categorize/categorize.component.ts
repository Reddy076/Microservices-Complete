import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import {
    LucideAngularModule, Tags, Loader2, CheckCircle2, AlertTriangle,
    Globe, User, FileText, Sparkles, FolderOpen
} from 'lucide-angular';

interface CategorizationResult {
    suggestedCategory: string;
    confidence: number;
    reasoning: string;
    alternativeCategories: string[];
}

@Component({
    selector: 'app-categorize',
    standalone: true,
    imports: [CommonModule, FormsModule, LucideAngularModule],
    templateUrl: './categorize.component.html',
    styleUrl: './categorize.component.css'
})
export class CategorizeComponent {
    readonly Tags = Tags;
    readonly Loader2 = Loader2;
    readonly CheckCircle2 = CheckCircle2;
    readonly AlertTriangle = AlertTriangle;
    readonly Globe = Globe;
    readonly User = User;
    readonly FileText = FileText;
    readonly Sparkles = Sparkles;
    readonly FolderOpen = FolderOpen;

    title: string = '';
    url: string = '';
    username: string = '';
    isAnalyzing: boolean = false;
    result: CategorizationResult | null = null;
    errorMessage: string | null = null;

    constructor(private http: HttpClient) { }

    categorize() {
        if (!this.title || this.title.trim() === '') {
            this.errorMessage = 'Title is required for categorization.';
            return;
        }

        this.isAnalyzing = true;
        this.result = null;
        this.errorMessage = null;

        this.http.post<CategorizationResult>(`${environment.apiBaseUrl}/api/ai/categorize-entry`, {
            title: this.title.trim(),
            url: this.url.trim() || null,
            username: this.username.trim() || null
        }).subscribe({
            next: (res) => {
                this.result = res;
                this.isAnalyzing = false;
            },
            error: (err) => {
                console.error('Categorization failed', err);
                this.errorMessage = 'Failed to categorize entry. Please try again.';
                this.isAnalyzing = false;
            }
        });
    }

    getConfidenceClass(confidence: number): string {
        if (confidence >= 0.8) return 'confidence-high';
        if (confidence >= 0.5) return 'confidence-medium';
        return 'confidence-low';
    }

    getConfidenceLabel(confidence: number): string {
        if (confidence >= 0.8) return 'High';
        if (confidence >= 0.5) return 'Medium';
        return 'Low';
    }

    reset() {
        this.title = '';
        this.url = '';
        this.username = '';
        this.result = null;
        this.errorMessage = null;
    }
}
