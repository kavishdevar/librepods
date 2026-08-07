//! MagicPods-style centered popup overlay, styled like the Windows 11 volume/
//! brightness flyout: a rounded card (native DWM corners), a subtle border, and
//! a theme-aware fill (dark card in dark mode, light in light mode). Appears near
//! the top-center on events (connect, ANC change, case open/close), shows the
//! AirPods name + a status line, and auto-hides after a few seconds.
//!
//! Created on the main (message-loop) thread via `init()`; `show()` is callable
//! from any thread — it stashes the text and posts a message to the window,
//! which the main thread's `GetMessage` loop dispatches to `wnd_proc`.

use std::sync::{Mutex, OnceLock};

use windows_sys::Win32::Foundation::{HWND, LPARAM, LRESULT, RECT, WPARAM};
use windows_sys::Win32::Graphics::Dwm::DwmSetWindowAttribute;
use windows_sys::Win32::Graphics::Gdi::{
    BeginPaint, CreateFontW, CreateSolidBrush, DeleteObject, DrawTextW, EndPaint, FillRect,
    InvalidateRect, PAINTSTRUCT, SelectObject, SetBkMode, SetTextColor,
};
use windows_sys::Win32::System::LibraryLoader::GetModuleHandleW;
use windows_sys::Win32::UI::WindowsAndMessaging::{
    CreateWindowExW, DefWindowProcW, GetSystemMetrics, KillTimer, PostMessageW, RegisterClassW,
    SM_CXSCREEN, SW_HIDE, SW_SHOWNA, SWP_NOACTIVATE, SetTimer, SetWindowPos, ShowWindow, WM_APP,
    WM_PAINT, WM_TIMER, WNDCLASSW, WS_EX_NOACTIVATE, WS_EX_TOOLWINDOW, WS_EX_TOPMOST, WS_POPUP,
};

const WIDTH: i32 = 380;
const HEIGHT: i32 = 96;
const TOP_MARGIN: i32 = 48;
const HIDE_TIMER_ID: usize = 1;
const HIDE_MS: u32 = 4500;
const WM_SHOW_OVERLAY: u32 = WM_APP + 1;

const HWND_TOPMOST: HWND = -1isize as HWND;
const DT_SINGLELINE: u32 = 0x20; // DrawText: one line
const BK_TRANSPARENT: i32 = 1; // SetBkMode: transparent
const FW_BOLD: i32 = 700;
const FW_NORMAL: i32 = 400;
const DEFAULT_CHARSET_U: u32 = 1;

// DWM window attributes (dwmapi).
const DWMWA_USE_IMMERSIVE_DARK_MODE: u32 = 20;
const DWMWA_WINDOW_CORNER_PREFERENCE: u32 = 33;
const DWMWA_BORDER_COLOR: u32 = 34;
const DWMWCP_ROUND: u32 = 2;

/// (title, subtitle) to render. `show()` sets it from any thread.
static CONTENT: Mutex<(String, String)> = Mutex::new((String::new(), String::new()));
/// The overlay window handle as usize (HWND isn't Send). Set once in `init()`.
static HWND_CELL: OnceLock<usize> = OnceLock::new();

fn to_utf16(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}

unsafe fn dwm_set_u32(hwnd: HWND, attr: u32, value: u32) {
    DwmSetWindowAttribute(hwnd, attr, &value as *const u32 as *const core::ffi::c_void, 4);
}

/// Apply the current Windows theme to the window chrome (immersive dark title/
/// border tone + a subtle border color). Called on each show so a theme switch
/// is reflected.
unsafe fn apply_theme(hwnd: HWND) {
    let dark = crate::theme::apps_dark();
    dwm_set_u32(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, dark as u32);
    // Subtle 1px border (COLORREF 0x00BBGGRR), a touch lighter/darker than the card.
    let border = if dark { 0x0045_4545 } else { 0x00D0_D0D0 };
    dwm_set_u32(hwnd, DWMWA_BORDER_COLOR, border);
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
            WS_EX_TOOLWINDOW | WS_EX_TOPMOST | WS_EX_NOACTIVATE,
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
        // Native rounded corners (Windows 11), like the system flyouts.
        dwm_set_u32(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, DWMWCP_ROUND);
        apply_theme(hwnd);
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
            apply_theme(hwnd); // pick up a theme switch since last shown
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

    // Theme-aware fill (COLORREF is 0x00BBGGRR). DWM clips the corners round, so
    // we just fill the whole client with the flyout card color. Dark card + light
    // text in dark mode; light card + dark text in light mode.
    let dark = crate::theme::apps_dark();
    let (card_col, title_col, sub_col) = if dark {
        (0x002B_2B2B, 0x00FF_FFFF, 0x00C8_C8C8)
    } else {
        (0x00F3_F3F3, 0x0020_2020, 0x0060_6060)
    };

    let full = RECT { left: 0, top: 0, right: WIDTH, bottom: HEIGHT };
    let bg = CreateSolidBrush(card_col);
    FillRect(hdc, &full, bg);
    DeleteObject(bg);

    let (title, subtitle) = CONTENT.lock().map(|c| c.clone()).unwrap_or_default();
    SetBkMode(hdc, BK_TRANSPARENT);

    let title_font = font(26, FW_BOLD);
    let old = SelectObject(hdc, title_font);
    draw_line(hdc, &title, 16, 52, title_col);
    SelectObject(hdc, old);
    DeleteObject(title_font);

    let sub_font = font(20, FW_NORMAL);
    let old = SelectObject(hdc, sub_font);
    draw_line(hdc, &subtitle, 52, HEIGHT - 12, sub_col);
    SelectObject(hdc, old);
    DeleteObject(sub_font);

    EndPaint(hwnd, &ps);
}
