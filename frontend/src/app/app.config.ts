import { ApplicationConfig, provideZoneChangeDetection, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { BASE_PATH } from './core/api';
import { environment } from '../environments/environment';
import { routes } from './app.routes';
import {
  LucideAngularModule, LayoutDashboard, Lock, Star, Trash2, Folder, Settings, LogOut, ChevronLeft, ChevronRight, Vault, Search, Check, RotateCw, ArrowRight, Plus, Eye, EyeOff, X, Copy, Edit2, ShieldAlert, Key, Smartphone, Laptop, MapPin, AlertTriangle, Calendar,
  User, Shield, Sliders,
  KeyRound, Sparkles, Monitor, Sun, Moon, ChevronDown, CalendarX, ShieldCheck, AlertCircle, CheckCircle, ShieldOff,
  BarChart2, Activity, TrendingUp, TrendingDown, Menu, Minus, Clock, ChevronUp, Lightbulb, Download, Upload, Info, HelpCircle, RotateCcw, UploadCloud, FileJson, File, Settings2, FolderOpen, Camera, History, Bell, LogIn, Share2, Mail, Link, Loader2, Users, UserPlus, PlusCircle, TriangleAlert
} from 'lucide-angular';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimations(),
    provideHttpClient(withInterceptors([
      authInterceptor,
      errorInterceptor
    ])),
    { provide: BASE_PATH, useValue: environment.apiBaseUrl },
    importProvidersFrom(LucideAngularModule.pick({
      LayoutDashboard, Lock, Star, Trash2, Folder, Settings, LogOut, ChevronLeft, ChevronRight, Vault, Search, Check, RotateCw, ArrowRight, Plus, Eye, EyeOff, X, Copy, Edit2, ShieldAlert, Key, Smartphone, Laptop, MapPin, AlertTriangle, Calendar,
      User, Shield, Sliders,
      KeyRound, Sparkles, Monitor, Sun, Moon, ChevronDown, CalendarX, ShieldCheck, AlertCircle, CheckCircle, ShieldOff,
      BarChart2, Activity, TrendingUp, TrendingDown, Menu, Minus, Clock, ChevronUp, Lightbulb, Download, Upload, Info, HelpCircle, RotateCcw, UploadCloud, FileJson, File, Settings2, FolderOpen, Camera, History, Bell, LogIn, Share2, Mail, Link, Loader2, Users, UserPlus, PlusCircle, TriangleAlert
    }))
  ]
};
