//! MagicPods-style centered popup overlay. A borderless, always-on-top,
//! click-through window that appears near the top-center of the screen on events
//! (connect, ANC change, case open/close), shows the AirPods name + a status
//! line, and auto-hides after a few seconds.
//!
//! Created on the main (message-loop) thread via `init()`; `show()` is callable
//! from any thread — it stashes the text and posts a message to the window,
//! which the main thread's `GetMessage` loop dispatches to `wnd_proc`.

use std::sync::{Mutex, OnceLock};

use windows_sys::Win32::Foundation::{HWND, LPARAM, LRESULT, RECT, WPARAM};
use windows_sys::Win32::Graphics::Gdi::{
    BeginPaint, CreateFontW, CreateSolidBrush, DeleteObject, DrawTextW, EndPaint, FillRect,
    InvalidateRect, PAINTSTRUCT, RoundRect, SelectObject, SetBkMode, SetTextColor,
};
use windows_sys::Win32::System::LibraryLoader::GetModuleHandleW;
use windows_sys::Win32::UI::WindowsAndMessaging::{
    CreateWindowExW, DefWindowProcW, GetSystemMetrics, KillTimer, LWA_COLORKEY, PostMessageW,
    RegisterClassW, SM_CXSCREEN, SW_HIDE, SW_SHOWNA, SWP_NOACTIVATE, SetLayeredWindowAttributes,
    SetTimer, SetWindowPos, ShowWindow, WM_APP, WM_PAINT, WM_TIMER, WNDCLASSW, WS_EX_LAYERED,
    WS_EX_NOACTIVATE, WS_EX_TOOLWINDOW, WS_EX_TOPMOST, WS_EX_TRANSPARENT, WS_POPUP,
};

const WIDTH: i32 = 380;
const HEIGHT: i32 = 96;
const TOP_MARGIN: i32 = 48;
const HIDE_TIMER_ID: usize = 1;
const HIDE_MS: u32 = 3200;
const WM_SHOW_OVERLAY: u32 = WM_APP + 1;

const HWND_TOPMOST: HWND = -1isize as HWND;
const DT_SINGLELINE: u32 = 0x20; // DrawText: one line
const BK_TRANSPARENT: i32 = 1; // SetBkMode: transparent
const FW_BOLD: i32 = 700;
const FW_NORMAL: i32 = 400;
const DEFAULT_CHARSET_U: u32 = 1;

/// (title, subtitle) to render. `show()` sets it from any thread.
static CONTENT: Mutex<(String, String)> = Mutex::new((String::new(), String::new()));
/// The overlay window handle as usize (HWND isn't Send). Set once in `init()`.
static HWND_CELL: OnceLock<usize> = OnceLock::new();

fn to_utf16(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}

/// Create the (hidden) overlay window. Call once on the message-loop thread.
pub fn init() {
    unsafe {
        let hinst = GetModuleHandleW(std::ptr::null());
        let class_name = to_utf16("LibrePodsOverlay");

        let wc = WNDCLASSW {
            style: 0,
            lpfnWndProc: Some(wnd_proc),
            cbClsExtra: 0,
            cbWndExtra: 0,
            hInstance: hinst,
            hIcon: std::ptr::null_mut(),
            hCursor: std::ptr::null_mut(),
            hbrBackground: std::ptr::null_mut(),
            lpszMenuName: std::ptr::null(),
            lpszClassName: class_name.as_ptr(),
        };
        RegisterClassW(&wc);

        let hwnd = CreateWindowExW(
            WS_EX_LAYERED | WS_EX_TOPMOST | WS_EX_TOOLWINDOW | WS_EX_NOACTIVATE | WS_EX_TRANSPARENT,
            class_name.as_ptr(),
            std::ptr::null(),
            WS_POPUP,
            0,
            0,
            WIDTH,
            HEIGHT,
            std::ptr::null_mut(),
            std::ptr::null_mut(),
            hinst,
            std::ptr::null(),
        );
        if hwnd.is_null() {
            return;
        }
        // Color-key pure black to transparent: the window fills black (invisible)
        // and only the non-black rounded card + text show — so it's a floating
        // pill, not a black square.
        SetLayeredWindowAttributes(hwnd, 0x0000_0000, 0, LWA_COLORKEY);
        let _ = HWND_CELL.set(hwnd as usize);
    }
}

/// Show the overlay with the given title + subtitle. Any thread; no-op if
/// `init()` hasn't run.
pub fn show(title: &str, subtitle: &str) {
    if let Ok(mut c) = CONTENT.lock() {
        *c = (title.to_string(), subtitle.to_string());
    }
    if let Some(&h) = HWND_CELL.get() {
        unsafe {
            PostMessageW(h as HWND, WM_SHOW_OVERLAY, 0, 0);
        }
    }
}

unsafe extern "system" fn wnd_proc(hwnd: HWND, msg: u32, wp: WPARAM, lp: LPARAM) -> LRESULT {
    match msg {
        WM_SHOW_OVERLAY => {
            let x = (GetSystemMetrics(SM_CXSCREEN) - WIDTH) / 2;
            SetWindowPos(hwnd, HWND_TOPMOST, x, TOP_MARGIN, WIDTH, HEIGHT, SWP_NOACTIVATE);
            ShowWindow(hwnd, SW_SHOWNA);
            InvalidateRect(hwnd, std::ptr::null(), 1);
            KillTimer(hwnd, HIDE_TIMER_ID);
            SetTimer(hwnd, HIDE_TIMER_ID, HIDE_MS, None);
            0
        }
        WM_TIMER if wp == HIDE_TIMER_ID => {
            KillTimer(hwnd, HIDE_TIMER_ID);
            ShowWindow(hwnd, SW_HIDE);
            0
        }
        WM_PAINT => {
            paint(hwnd);
            0
        }
        _ => DefWindowProcW(hwnd, msg, wp, lp),
    }
}

unsafe fn font(height: i32, weight: i32) -> windows_sys::Win32::Graphics::Gdi::HFONT {
    CreateFontW(
        height, 0, 0, 0, weight, 0, 0, 0, DEFAULT_CHARSET_U, 0, 0, 0, 0,
        to_utf16("Segoe UI").as_ptr(),
    )
}

unsafe fn draw_line(hdc: *mut core::ffi::c_void, text: &str, top: i32, bottom: i32, color: u32) {
    let mut r = RECT { left: 24, top, right: WIDTH - 24, bottom };
    let mut w = to_utf16(text);
    SetTextColor(hdc, color);
    DrawTextW(hdc, w.as_mut_ptr(), -1, &mut r, DT_SINGLELINE);
}

unsafe fn paint(hwnd: HWND) {
    let mut ps: PAINTSTRUCT = std::mem::zeroed();
    let hdc = BeginPaint(hwnd, &mut ps);

    // Opaque black backdrop (the layered alpha softens the edges).
    let full = RECT { left: 0, top: 0, right: WIDTH, bottom: HEIGHT };
    let bg = CreateSolidBrush(0x00000000);
    FillRect(hdc, &full, bg);
    DeleteObject(bg);

    // Rounded dark card (COLORREF is 0x00BBGGRR).
    let card = CreateSolidBrush(0x0022262A);
    let old_brush = SelectObject(hdc, card);
    RoundRect(hdc, 6, 6, WIDTH - 6, HEIGHT - 6, 28, 28);
    SelectObject(hdc, old_brush);
    DeleteObject(card);

    let (title, subtitle) = CONTENT.lock().map(|c| c.clone()).unwrap_or_default();
    SetBkMode(hdc, BK_TRANSPARENT);

    let title_font = font(26, FW_BOLD);
    let old = SelectObject(hdc, title_font);
    draw_line(hdc, &title, 16, 52, 0x00FFFFFF); // white
    SelectObject(hdc, old);
    DeleteObject(title_font);

    let sub_font = font(20, FW_NORMAL);
    let old = SelectObject(hdc, sub_font);
    draw_line(hdc, &subtitle, 52, HEIGHT - 12, 0x00B8B8B8); // gray
    SelectObject(hdc, old);
    DeleteObject(sub_font);

    EndPaint(hwnd, &ps);
}
