//! Bridge to the LibrePodsAAP kernel driver. Cloneable + thread-safe so the
//! background receive loop and the tray's ANC-send can share one handle.

use std::ffi::c_void;
use std::io;
use std::ptr;
use std::sync::Arc;

use windows_sys::Win32::Devices::DeviceAndDriverInstallation::{
    DIGCF_DEVICEINTERFACE, DIGCF_PRESENT, SP_DEVICE_INTERFACE_DATA,
    SP_DEVICE_INTERFACE_DETAIL_DATA_W, SetupDiDestroyDeviceInfoList, SetupDiEnumDeviceInterfaces,
    SetupDiGetClassDevsW, SetupDiGetDeviceInterfaceDetailW,
};
use windows_sys::Win32::Foundation::{CloseHandle, HANDLE, INVALID_HANDLE_VALUE};
use windows_sys::Win32::Storage::FileSystem::{
    CreateFileW, FILE_SHARE_READ, FILE_SHARE_WRITE, OPEN_EXISTING,
};
use windows_sys::Win32::System::IO::DeviceIoControl;
use windows_sys::core::GUID;

const GENERIC_READ: u32 = 0x8000_0000;
const GENERIC_WRITE: u32 = 0x4000_0000;

const GUID_DEVINTERFACE_LIBREPODSAAP: GUID = GUID {
    data1: 0xC0FF_EE00,
    data2: 0x1337,
    data3: 0x4A5B,
    data4: [0x9E, 0x6F, 0xA1, 0xB2, 0xC3, 0xD4, 0xE5, 0xF6],
};

const IOCTL_LP_CONNECT: u32 = 0x8000_2000;
const IOCTL_LP_SEND: u32 = 0x8000_2008;
const IOCTL_LP_RECEIVE: u32 = 0x8000_200C;

struct DriverHandle(HANDLE);
unsafe impl Send for DriverHandle {}
unsafe impl Sync for DriverHandle {}
impl Drop for DriverHandle {
    fn drop(&mut self) {
        unsafe { CloseHandle(self.0) };
    }
}

#[derive(Clone)]
pub struct Driver {
    handle: Arc<DriverHandle>,
}

fn ioctl(handle: HANDLE, code: u32, input: &[u8], output: &mut [u8]) -> io::Result<u32> {
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
        if DeviceIoControl(
            handle, code, in_ptr, input.len() as u32, out_ptr, output.len() as u32, &mut returned,
            ptr::null_mut(),
        ) == 0
        {
            return Err(io::Error::last_os_error());
        }
        Ok(returned)
    }
}

impl Driver {
    pub fn open() -> io::Result<Driver> {
        let handle = open_driver()?;
        Ok(Driver {
            handle: Arc::new(DriverHandle(handle)),
        })
    }

    pub fn connect(&self, addr: u64, psm: u16) -> io::Result<bool> {
        let mut input = [0u8; 10];
        input[0..8].copy_from_slice(&addr.to_le_bytes());
        input[8..10].copy_from_slice(&psm.to_le_bytes());
        let mut out = [0u8; 8];
        ioctl(self.handle.0, IOCTL_LP_CONNECT, &input, &mut out)?;
        Ok(u32::from_le_bytes([out[0], out[1], out[2], out[3]]) != 0)
    }

    pub fn send(&self, data: &[u8]) -> io::Result<()> {
        ioctl(self.handle.0, IOCTL_LP_SEND, data, &mut [])?;
        Ok(())
    }

    pub fn recv(&self, timeout_ms: u32, buf: &mut [u8]) -> io::Result<usize> {
        let to = timeout_ms.to_le_bytes();
        Ok(ioctl(self.handle.0, IOCTL_LP_RECEIVE, &to, buf)? as usize)
    }
}

fn open_driver() -> io::Result<HANDLE> {
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
                "LibrePodsAAP driver not found (installed and bound to the AirPods?)",
            ));
        }

        let mut required: u32 = 0;
        SetupDiGetDeviceInterfaceDetailW(
            devinfo, &ifdata, ptr::null_mut(), 0, &mut required, ptr::null_mut(),
        );

        let mut buf = vec![0u8; required as usize];
        let detail = buf.as_mut_ptr() as *mut SP_DEVICE_INTERFACE_DETAIL_DATA_W;
        (*detail).cbSize = if cfg!(target_pointer_width = "64") { 8 } else { 6 };

        if SetupDiGetDeviceInterfaceDetailW(
            devinfo, &ifdata, detail, required, ptr::null_mut(), ptr::null_mut(),
        ) == 0
        {
            SetupDiDestroyDeviceInfoList(devinfo);
            return Err(io::Error::last_os_error());
        }

        let path = (*detail).DevicePath.as_ptr();
        let handle = CreateFileW(
            path,
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
        Ok(handle)
    }
}
