import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { LucideAngularModule, ShieldCheck, KeyRound, Bug, Lightbulb, TriangleAlert, CircleX, CheckCircle2, ArrowRight, Shield, Loader2 } from 'lucide-angular';

interface AnalysisResult {
    strength: string;
    vulnerabilities: string[];
    suggestions: string[];
}

@Component({
    selector: 'app-password-analyzer',
    standalone: true,
    imports: [CommonModule, FormsModule, LucideAngularModule],
    templateUrl: './password-analyzer.component.html',
    styleUrl: './password-analyzer.component.css'
})
export class PasswordAnalyzerComponent {
    readonly ShieldCheck = ShieldCheck;
    readonly KeyRound = KeyRound;
    readonly Bug = Bug;
    readonly Lightbulb = Lightbulb;
    readonly TriangleAlert = TriangleAlert;
    readonly CircleX = CircleX;
    readonly CheckCircle2 = CheckCircle2;
    readonly ArrowRight = ArrowRight;
    readonly Shield = Shield;
    readonly Loader2 = Loader2;

    passwordInput: string = '';
    isAnalyzing: boolean = false;
    analysisResult: AnalysisResult | null = null;
    errorMessage: string | null = null;

    constructor(private http: HttpClient) { }

    analyzePassword() {
        if (!this.passwordInput || this.passwordInput.trim() === '') {
            return;
        }

        this.isAnalyzing = true;
        this.analysisResult = null;
        this.errorMessage = null;

        this.http.post<AnalysisResult>(`${environment.apiBaseUrl}/api/ai/analyze-password`, {
            password: this.passwordInput
        }).subscribe({
            next: (result) => {
                this.analysisResult = result;
                this.isAnalyzing = false;
            },
            error: (err) => {
                console.error('Analysis failed', err);
                this.errorMessage = 'Failed to analyze password. Please try again later.';
                this.isAnalyzing = false;
            }
        });
    }

    getStrengthBadgeClass(strength: string): string {
        switch (strength) {
            case 'VERY_WEAK': return 'bg-danger';
            case 'WEAK': return 'bg-warning text-dark';
            case 'MODERATE': return 'bg-info text-dark';
            case 'STRONG': return 'bg-primary';
            case 'VERY_STRONG': return 'bg-success';
            default: return 'bg-secondary';
        }
    }
}
