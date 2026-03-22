import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import {
    LucideAngularModule, MessageCircle, Send, Bot, User, Loader2,
    ShieldCheck, AlertTriangle, Info, Sparkles
} from 'lucide-angular';

interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
    timestamp: Date;
}

interface ChatResponse {
    reply: string;
    suggestions: string[];
    conversationId: string;
}

@Component({
    selector: 'app-chatbot',
    standalone: true,
    imports: [CommonModule, FormsModule, LucideAngularModule],
    templateUrl: './chatbot.component.html',
    styleUrl: './chatbot.component.css'
})
export class ChatbotComponent {
    readonly MessageCircle = MessageCircle;
    readonly Send = Send;
    readonly Bot = Bot;
    readonly User = User;
    readonly Loader2 = Loader2;
    readonly ShieldCheck = ShieldCheck;
    readonly AlertTriangle = AlertTriangle;
    readonly Info = Info;
    readonly Sparkles = Sparkles;

    userInput: string = '';
    messages: ChatMessage[] = [];
    isLoading: boolean = false;
    conversationId: string = '';
    errorMessage: string | null = null;

    quickPrompts: string[] = [
        'How do I create a strong password?',
        'What is two-factor authentication?',
        'How to detect phishing emails?',
        'Best practices for password management',
        'What should I do if my account is hacked?'
    ];

    constructor(private http: HttpClient) {
        this.messages.push({
            role: 'assistant',
            content: 'Hello! I\'m your AI Security Assistant. I can help you with password security, best practices, and general cybersecurity questions. How can I help you today?',
            timestamp: new Date()
        });
    }

    sendMessage() {
        if (!this.userInput || this.userInput.trim() === '' || this.isLoading) {
            return;
        }

        const userMsg = this.userInput.trim();
        this.messages.push({
            role: 'user',
            content: userMsg,
            timestamp: new Date()
        });

        this.userInput = '';
        this.isLoading = true;
        this.errorMessage = null;

        this.http.post<ChatResponse>(`${environment.apiBaseUrl}/api/ai/chat`, {
            message: userMsg,
            conversationId: this.conversationId || null
        }).subscribe({
            next: (result) => {
                this.messages.push({
                    role: 'assistant',
                    content: result.reply,
                    timestamp: new Date()
                });
                if (result.conversationId) {
                    this.conversationId = result.conversationId;
                }
                this.isLoading = false;
            },
            error: (err) => {
                console.error('Chat failed', err);
                this.errorMessage = 'Failed to get a response. Please try again.';
                this.messages.push({
                    role: 'assistant',
                    content: 'Sorry, I encountered an error. Please try again later.',
                    timestamp: new Date()
                });
                this.isLoading = false;
            }
        });
    }

    useQuickPrompt(prompt: string) {
        this.userInput = prompt;
        this.sendMessage();
    }

    clearChat() {
        this.messages = [{
            role: 'assistant',
            content: 'Chat cleared. How can I help you?',
            timestamp: new Date()
        }];
        this.conversationId = '';
        this.errorMessage = null;
    }
}
