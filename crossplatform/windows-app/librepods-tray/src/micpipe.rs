//! Writer for the LibrePodsMic virtual microphone: pushes decoded PCM into the
//! driver's ring buffer over the control device `\\.\LibrePodsMic`.

use std::ffi::c_void;
use std::ptr;

use windows_sys::Win32::Foundation::{CloseHandle, HANDLE, INVALID_HANDLE_VALUE};
use windows_sys::Win32::Storage::FileSystem::{
    CreateFileW, FILE_SHARE_READ, FILE_SHARE_WRITE, OPEN_EXISTING,
};
use windows_sys::Win32::System::IO::DeviceIoControl;

const GENERIC_READ: u32 = 0x8000_0000;
const GENERIC_WRITE: u32 = 0x4000_0000;
// CTL_CODE(FILE_DEVICE_UNKNOWN, 0x800, METHOD_BUFFERED, FILE_WRITE_DATA)
const IOCTL_LIBREPODS_MIC_WRITE_PCM: u32 = 0x0022_A000;
// CTL_CODE(FILE_DEVICE_UNKNOWN, 0x801, METHOD_BUFFERED, FILE_READ_DATA)
const IOCTL_LIBREPODS_MIC_STATUS: u32 = 0x0022_6004;

pub struct MicPipe {
    handle: HANDLE,
}

// One tray owns the single exclusive handle and uses it from the receive thread
// (writes) and the status-poll thread (reads); both DeviceIoControl paths are
// independent, so sharing the handle is safe.
unsafe impl Send for MicPipe {}
unsafe impl Sync for MicPipe {}

impl MicPipe {
    /// Open the virtual-mic control device. None if the LibrePodsMic driver
    /// isn't installed (or another writer holds the exclusive handle).
    pub fn open() -> Option<MicPipe> {
        let path: Vec<u16> = r"\\.\LibrePodsMic"
            .encode_utf16()
            .chain(std::iter::once(0))
            .collect();
        let handle = unsafe {
            CreateFileW(
                path.as_ptr(),
                GENERIC_READ | GENERIC_WRITE,
                FILE_SHARE_READ | FILE_SHARE_WRITE,
                ptr::null(),
                OPEN_EXISTING,
                0,
                ptr::null_mut(),
            )
        };
        if handle == INVALID_HANDLE_VALUE || handle.is_null() {
            None
        } else {
            Some(MicPipe { handle })
        }
    }

    /// Capture-activity counter — advances while an app is recording from the
    /// virtual mic. The tray polls it to auto-enable/disable the hi-res stream.
    pub fn status(&self) -> i32 {
        let mut out = [0u8; 4];
        let mut returned = 0u32;
        unsafe {
            DeviceIoControl(
                self.handle,
                IOCTL_LIBREPODS_MIC_STATUS,
                ptr::null(),
                0,
                out.as_mut_ptr() as *mut c_void,
                4,
                &mut returned,
                ptr::null_mut(),
            );
        }
        i32::from_le_bytes(out)
    }

    /// Push mono 16-bit PCM samples into the mic ring.
    pub fn write(&self, samples: &[i16]) {
        if samples.is_empty() {
            return;
        }
        let bytes = std::mem::size_of_val(samples);
        let mut returned = 0u32;
        unsafe {
            DeviceIoControl(
                self.handle,
                IOCTL_LIBREPODS_MIC_WRITE_PCM,
                samples.as_ptr() as *const c_void,
                bytes as u32,
                ptr::null_mut(),
                0,
                &mut returned,
                ptr::null_mut(),
            );
        }
    }
}

impl Drop for MicPipe {
    fn drop(&mut self) {
        unsafe { CloseHandle(self.handle) };
    }
}
