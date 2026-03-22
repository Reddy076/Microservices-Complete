import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { NotificationBellComponent } from './notification-bell.component';
import { NotificationControllerService } from '../../core/api/api/notificationController.service';
import { NotificationEventService } from '../../core/services/notification-event.service';
import { of, throwError, EMPTY } from 'rxjs';
import { NotificationDTO } from '../../core/api/model/notificationDTO';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

const makeNotif = (overrides: Partial<NotificationDTO> = {}): NotificationDTO => ({
  id: 1,
  title: 'Test Notification',
  message: 'This is a test message.',
  notificationType: 'INFO',
  createdAt: new Date().toISOString(),
  read: false,
  ...overrides,
});

describe('NotificationBellComponent', () => {
  let component: NotificationBellComponent;
  let fixture: ComponentFixture<NotificationBellComponent>;
  let notifService: jasmine.SpyObj<NotificationControllerService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('NotificationControllerService', [
      'getUnreadCount',
      'getNotifications',
      'markAsRead',
      'markAllAsRead',
      'deleteNotification',
    ]);

    // Default happy-path returns
    spy.getUnreadCount.and.returnValue(of({ unreadCount: 3 }) as any);
    spy.getNotifications.and.returnValue(of([makeNotif()]) as any);
    spy.markAsRead.and.returnValue(of({ message: 'ok' }) as any);
    spy.markAllAsRead.and.returnValue(of({ message: 'ok' }) as any);
    spy.deleteNotification.and.returnValue(of({ message: 'ok' }) as any);

    // Stub NotificationEventService so triggerRefresh() doesn't cause the
    // polling subscription to re-fetch unreadCount (which would overwrite
    // values set directly in tests).
    const eventServiceStub = {
      refresh$: EMPTY,
      triggerRefresh: jasmine.createSpy('triggerRefresh')
    };

    await TestBed.configureTestingModule({
      imports: [NotificationBellComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationControllerService, useValue: spy },
        { provide: NotificationEventService, useValue: eventServiceStub },
      ],
    }).compileComponents();

    notifService = TestBed.inject(
      NotificationControllerService
    ) as jasmine.SpyObj<NotificationControllerService>;

    fixture = TestBed.createComponent(NotificationBellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call getUnreadCount on init and set unreadCount', fakeAsync(() => {
    tick();
    expect(notifService.getUnreadCount).toHaveBeenCalled();
    expect(component.unreadCount()).toBe(3);
  }));

  it('hasUnread should be true when unreadCount > 0', fakeAsync(() => {
    tick();
    expect(component.hasUnread()).toBeTrue();
  }));

  it('hasUnread should be false when unreadCount is 0', fakeAsync(() => {
    notifService.getUnreadCount.and.returnValue(of({ unreadCount: 0 }) as any);
    component.ngOnDestroy(); // cancel existing subscription first
    component.ngOnInit();
    tick();
    expect(component.hasUnread()).toBeFalse();
    discardPeriodicTasks(); // clean up the 30s interval
  }));

  it('should open dropdown and load notifications on toggleDropdown', fakeAsync(() => {
    component.toggleDropdown(new MouseEvent('click'));
    tick();
    expect(notifService.getNotifications).toHaveBeenCalled();
    expect(component.dropdownOpen()).toBeTrue();
    expect(component.notifications().length).toBe(1);
  }));

  it('should close dropdown on second toggleDropdown call', fakeAsync(() => {
    component.toggleDropdown(new MouseEvent('click'));
    tick();
    component.toggleDropdown(new MouseEvent('click'));
    expect(component.dropdownOpen()).toBeFalse();
  }));

  it('markAsRead should update notification and decrement unreadCount', fakeAsync(() => {
    component.toggleDropdown(new MouseEvent('click'));
    tick();
    const n = makeNotif({ id: 1, read: false });
    component.notifications.set([n]);
    component.unreadCount.set(1);
    component.markAsRead(n, new MouseEvent('click'));
    tick();
    expect(notifService.markAsRead).toHaveBeenCalledWith(1);
    expect(component.notifications()[0].read).toBeTrue();
    expect(component.unreadCount()).toBe(0);
  }));

  it('markAsRead should do nothing if notification is already read', fakeAsync(() => {
    const n = makeNotif({ id: 2, read: true });
    component.markAsRead(n, new MouseEvent('click'));
    tick();
    expect(notifService.markAsRead).not.toHaveBeenCalled();
  }));

  it('markAllAsRead should mark all notifications read and reset unreadCount', fakeAsync(() => {
    component.notifications.set([makeNotif({ id: 1 }), makeNotif({ id: 2 })]);
    component.unreadCount.set(2);
    component.markAllAsRead(new MouseEvent('click'));
    tick();
    expect(notifService.markAllAsRead).toHaveBeenCalled();
    expect(component.notifications().every((n) => n.read)).toBeTrue();
    expect(component.unreadCount()).toBe(0);
  }));

  it('deleteNotification should remove the notification and decrement unreadCount if unread', fakeAsync(() => {
    const n = makeNotif({ id: 5, read: false });
    component.notifications.set([n]);
    component.unreadCount.set(1);
    component.deleteNotification(n, new MouseEvent('click'));
    tick();
    expect(notifService.deleteNotification).toHaveBeenCalledWith(5);
    expect(component.notifications().length).toBe(0);
    expect(component.unreadCount()).toBe(0);
  }));

  it('deleteNotification should NOT decrement unreadCount if notification was already read', fakeAsync(() => {
    const n = makeNotif({ id: 6, read: true });
    component.notifications.set([n]);
    component.unreadCount.set(0);
    component.deleteNotification(n, new MouseEvent('click'));
    tick();
    expect(component.notifications().length).toBe(0);
    expect(component.unreadCount()).toBe(0);
  }));

  it('should handle getUnreadCount error gracefully', fakeAsync(() => {
    notifService.getUnreadCount.and.returnValue(throwError(() => new Error('Network error')) as any);
    expect(() => {
      component.ngOnInit();
      tick();
    }).not.toThrow();
  }));

  it('should handle getNotifications error gracefully', fakeAsync(() => {
    notifService.getNotifications.and.returnValue(throwError(() => new Error('fail')) as any);
    component.toggleDropdown(new MouseEvent('click'));
    tick();
    expect(component.loading()).toBeFalse();
  }));

  it('formatTime should return "just now" for very recent dates', () => {
    const recent = new Date(Date.now() - 30_000).toISOString();
    expect(component.formatTime(recent)).toBe('just now');
  });

  it('formatTime should return minutes ago', () => {
    const fiveMinsAgo = new Date(Date.now() - 5 * 60_000).toISOString();
    expect(component.formatTime(fiveMinsAgo)).toBe('5m ago');
  });

  it('formatTime should return hours ago', () => {
    const twoHoursAgo = new Date(Date.now() - 2 * 3600_000).toISOString();
    expect(component.formatTime(twoHoursAgo)).toBe('2h ago');
  });

  it('formatTime should return days ago', () => {
    const threeDaysAgo = new Date(Date.now() - 3 * 86400_000).toISOString();
    expect(component.formatTime(threeDaysAgo)).toBe('3d ago');
  });

  it('formatTime should return empty string for undefined input', () => {
    expect(component.formatTime(undefined)).toBe('');
  });

  it('onDocumentClick should close dropdown when clicking outside', fakeAsync(() => {
    component.dropdownOpen.set(true);
    const externalDiv = document.createElement('div');
    document.body.appendChild(externalDiv);
    const event = new MouseEvent('click', { bubbles: true });
    Object.defineProperty(event, 'target', { value: externalDiv });
    component.onDocumentClick(event);
    expect(component.dropdownOpen()).toBeFalse();
    document.body.removeChild(externalDiv);
  }));

  afterEach(() => {
    component.ngOnDestroy();
  });
});
