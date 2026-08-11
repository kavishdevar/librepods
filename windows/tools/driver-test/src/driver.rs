//! Thin user-mode wrapper over the LibrePodsAAP driver's IOCTL interface.

use std::ffi::c_void;
use std::io;
use std::ptr;

use windows_sys::core::GUID;
use windows_sys::Win32::Devices::DeviceAndDriverInstallation::{
    SetupDiDestroyDeviceInfoList, SetupDiEnumDeviceInterfaces, SetupDiGetClassDevsW,
    SetupDiGetDeviceInterfaceDetailW, DIGCF_DEVICEINTERFACE, DIGCF_PRESENT,
    SP_DEVICE_INTERFACE_DATA, SP_DEVICE_INTERFACE_DETAIL_DATA_W,
};
use windows_sys::Win32::Foundation::{CloseHandle, HANDLE, INVALID_HANDLE_VALUE};
use windows_sys::Win32::Storage::FileSystem::{
    CreateFileW, FILE_SHARE_READ, FILE_SHARE_WRITE, OPEN_EXISTING,
};
use windows_sys::Win32::System::IO::DeviceIoControl;

const GENERIC_READ: u32 = 0x8000_0000;
const GENERIC_WRITE: u32 = 0x4000_0000;

/// {C0FFEE00-1337-4A5B-9E6F-A1B2C3D4E5F6} — must match the driver's header.
const GUID_DEVINTERFACE_LIBREPODSAAP: GUID = GUID {
    data1: 0xC0FF_EE00,
    data2: 0x1337,
    data3: 0x4A5B,
    data4: [0x9E, 0x6F, 0xA1, 0xB2, 0xC3, 0xD4, 0xE5, 0xF6],
};

// CTL_CODE(0x8000, fn, METHOD_BUFFERED, FILE_ANY_ACCESS)
const IOCTL_LP_CONNECT: u32 = 0x8000_2000;
const IOCTL_LP_DISCONNECT: u32 = 0x8000_2004;
const IOCTL_LP_SEND: u32 = 0x8000_2008;
const IOCTL_LP_RECEIVE: u32 = 0x8000_200C;
const IOCTL_LP_GET_STATUS: u32 = 0x8000_2010;

#[repr(C, packed)]
struct ConnectInput {
    addr: u64,
    psm: u16,
}
#[repr(C, packed)]
struct ConnectOutput {
    success: u32,
    status: i32,
}
#[repr(C, packed)]
struct StatusOutput {
    state: u32,
    addr: u64,
}

pub struct Driver {
    handle: HANDLE,
}

impl Driver {
    /// Locate the driver's device interface and open a handle. Fails if the
    /// driver is not installed or not bound to the AirPods AAP service PDO.
    pub fn open() -> io::Result<Driver> {
        unsafe {
            let devinfo = SetupDiGetClassDevsW(
                &GUID_DEVINTERFACE_LIBREPODSAAP,
                ptr::null(),
                ptr::null_mut(),
                DIGCF_PRESENT | DIGCF_DEVICEINTERFACE,
            );
            if devinfo == INVALID_HANDLE_VALUE as isize {
                return Err(io::Error::last_os_error());
            }

            let mut ifdata: SP_DEVICE_INTERFACE_DATA = std::mem::zeroed();
            ifdata.cbSize = std::mem::size_of::<SP_DEVICE_INTERFACE_DATA>() as u32;

            if SetupDiEnumDeviceInterfaces(
                devinfo,
                ptr::null(),
                &GUID_DEVINTERFACE_LIBREPODSAAP,
                0,
                &mut ifdata,
            ) == 0
            {
                SetupDiDestroyDeviceInfoList(devinfo);
                return Err(io::Error::new(
                    io::ErrorKind::NotFound,
                    "LibrePodsAAP device interface not found — is the driver installed and bound to the AirPods?",
                ));
            }

            // Two-call idiom: first get the required detail buffer size.
            let mut required: u32 = 0;
            SetupDiGetDeviceInterfaceDetailW(
                devinfo,
                &ifdata,
                ptr::null_mut(),
                0,
                &mut required,
                ptr::null_mut(),
            );

            let mut buf = vec![0u8; required as usize];
            let detail = buf.as_mut_ptr() as *mut SP_DEVICE_INTERFACE_DETAIL_DATA_W;
            // cbSize is the size of the fixed part: 8 on x64, 6 on x86.
            (*detail).cbSize = if cfg!(target_pointer_width = "64") { 8 } else { 6 };

            if SetupDiGetDeviceInterfaceDetailW(
                devinfo,
                &ifdata,
                detail,
                required,
                ptr::null_mut(),
                ptr::null_mut(),
            ) == 0
            {
                SetupDiDestroyDeviceInfoList(devinfo);
                return Err(io::Error::last_os_error());
            }

            let path_ptr = (*detail).DevicePath.as_ptr();
            let handle = CreateFileW(
                path_ptr,
                GENERIC_READ | GENERIC_WRITE,
                FILE_SHARE_READ | FILE_SHARE_WRITE,
                ptr::null(),
                OPEN_EXISTING,
                0,
                ptr::null_mut(),
            );
            SetupDiDestroyDeviceInfoList(devinfo);

            if handle == INVALID_HANDLE_VALUE {
                return Err(io::Error::last_os_error());
            }
            Ok(Driver { handle })
        }
    }

    fn ioctl(&self, code: u32, input: &[u8], output: &mut [u8]) -> io::Result<u32> {
        unsafe {
            let mut returned: u32 = 0;
            let in_ptr = if input.is_empty() {
                ptr::null()
            } else {
                input.as_ptr() as *const c_void
            };
            let out_ptr = if output.is_empty() {
                ptr::null_mut()
            } else {
                output.as_mut_ptr() as *mut c_void
            };
            let ok = DeviceIoControl(
                self.handle,
                code,
                in_ptr,
                input.len() as u32,
                out_ptr,
                output.len() as u32,
                &mut returned,
                ptr::null_mut(),
            );
            if ok == 0 {
                return Err(io::Error::last_os_error());
            }
            Ok(returned)
        }
    }

    pub fn connect(&self, addr: u64, psm: u16) -> io::Result<(bool, i32)> {
        let input = ConnectInput { addr, psm };
        let bytes = unsafe {
            std::slice::from_raw_parts(
                &input as *const _ as *const u8,
                std::mem::size_of::<ConnectInput>(),
            )
        };
        let mut out = [0u8; 8];
        self.ioctl(IOCTL_LP_CONNECT, bytes, &mut out)?;
        let o = unsafe { &*(out.as_ptr() as *const ConnectOutput) };
        Ok((o.success != 0, o.status))
    }

    pub fn disconnect(&self) -> io::Result<()> {
        self.ioctl(IOCTL_LP_DISCONNECT, &[], &mut [])?;
        Ok(())
    }

    pub fn send(&self, data: &[u8]) -> io::Result<()> {
        self.ioctl(IOCTL_LP_SEND, data, &mut [])?;
        Ok(())
    }

    pub fn receive(&self, timeout_ms: u32, buf: &mut [u8]) -> io::Result<usize> {
        let to = timeout_ms.to_le_bytes();
        let n = self.ioctl(IOCTL_LP_RECEIVE, &to, buf)?;
        Ok(n as usize)
    }

    pub fn status(&self) -> io::Result<(u32, u64)> {
        let mut out = [0u8; 12];
        self.ioctl(IOCTL_LP_GET_STATUS, &[], &mut out)?;
        let o = unsafe { &*(out.as_ptr() as *const StatusOutput) };
        Ok((o.state, o.addr))
    }
}

impl Drop for Driver {
    fn drop(&mut self) {
        unsafe {
            CloseHandle(self.handle);
        }
    }
}
