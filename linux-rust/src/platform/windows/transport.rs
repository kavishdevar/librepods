//! Windows L2CAP transport: bridges to the LibrePodsAAP kernel driver via
//! DeviceIoControl. Mirrors the proven `lp-driver-test` bridge. DeviceIoControl
//! is synchronous, so the async trait methods hop onto a blocking thread.

use crate::platform::{DeviceId, L2capTransport};
use std::ffi::c_void;
use std::io;
use std::ptr;
use std::sync::Arc;
use std::time::Duration;

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

/// Must match the driver's GUID_DEVINTERFACE_LIBREPODSAAP.
const GUID_DEVINTERFACE_LIBREPODSAAP: GUID = GUID {
    data1: 0xC0FF_EE00,
    data2: 0x1337,
    data3: 0x4A5B,
    data4: [0x9E, 0x6F, 0xA1, 0xB2, 0xC3, 0xD4, 0xE5, 0xF6],
};

const IOCTL_LP_CONNECT: u32 = 0x8000_2000;
const IOCTL_LP_SEND: u32 = 0x8000_2008;
const IOCTL_LP_RECEIVE: u32 = 0x8000_200C;

/// Driver-side recv timeout (ms). Kept short so a blocking read doesn't stall
/// pending sends for long on the driver's sequential queue.
const RECV_TIMEOUT_MS: u32 = 1500;

/// Owns the driver handle. Safe to share: the driver serializes IOCTLs and
/// DeviceIoControl is thread-safe on a single handle.
struct DriverHandle(HANDLE);
unsafe impl Send for DriverHandle {}
unsafe impl Sync for DriverHandle {}
impl Drop for DriverHandle {
    fn drop(&mut self) {
        unsafe { CloseHandle(self.0) };
    }
}

pub struct WindowsL2cap {
    handle: Arc<DriverHandle>,
}

fn device_ioctl(handle: HANDLE, code: u32, input: &[u8], output: &mut [u8]) -> io::Result<u32> {
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
            handle,
            code,
            in_ptr,
            input.len() as u32,
            out_ptr,
            output.len() as u32,
            &mut returned,
            ptr::null_mut(),
        ) == 0
        {
            return Err(io::Error::last_os_error());
        }
        Ok(returned)
    }
}

/// Locate the driver's device interface and open a handle.
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
                "LibrePodsAAP driver not found — installed and bound to the AirPods?",
            ));
        }

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

        let path = (*detail).DevicePath.as_ptr();
        let handle = CreateFileW(
            path,
            GENERIC_READ | GENERIC_WRITE,
            FILE_SHARE_READ | FILE_SHARE_WRITE,
            ptr::null(),
            OPEN_EXISTING,
            0,
            0,
        );
        SetupDiDestroyDeviceInfoList(devinfo);

        if handle == INVALID_HANDLE_VALUE {
            return Err(io::Error::last_os_error());
        }
        Ok(handle)
    }
}

#[async_trait::async_trait]
impl L2capTransport for WindowsL2cap {
    async fn send(&self, data: &[u8]) -> io::Result<usize> {
        let handle = self.handle.clone();
        let data = data.to_vec();
        tokio::task::spawn_blocking(move || {
            device_ioctl(handle.0, IOCTL_LP_SEND, &data, &mut [])?;
            Ok(data.len())
        })
        .await
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e))?
    }

    async fn recv(&self, buf: &mut [u8]) -> io::Result<usize> {
        let handle = self.handle.clone();
        let len = buf.len();
        let out = tokio::task::spawn_blocking(move || {
            let mut out = vec![0u8; len];
            let timeout = RECV_TIMEOUT_MS.to_le_bytes();
            let n = device_ioctl(handle.0, IOCTL_LP_RECEIVE, &timeout, &mut out)?;
            out.truncate(n as usize);
            Ok::<Vec<u8>, io::Error>(out)
        })
        .await
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e))??;

        buf[..out.len()].copy_from_slice(&out);
        Ok(out.len())
    }
}

/// Open the AAP L2CAP channel to `addr` on `psm` through the driver. The
/// `timeout` is unused here — the driver applies its own connect timeout.
pub async fn l2cap_connect(
    addr: DeviceId,
    psm: u16,
    _timeout: Duration,
) -> io::Result<Arc<dyn L2capTransport>> {
    let bt_addr: u64 = addr.into();

    tokio::task::spawn_blocking(move || {
        let handle = open_driver()?;

        // LP_CONNECT_INPUT { u64 addr; u16 psm } (packed) = 10 bytes.
        let mut input = [0u8; 10];
        input[0..8].copy_from_slice(&bt_addr.to_le_bytes());
        input[8..10].copy_from_slice(&psm.to_le_bytes());

        // LP_CONNECT_OUTPUT { u32 success; i32 status }.
        let mut out = [0u8; 8];
        if let Err(e) = device_ioctl(handle, IOCTL_LP_CONNECT, &input, &mut out) {
            unsafe { CloseHandle(handle) };
            return Err(e);
        }

        let success = u32::from_le_bytes([out[0], out[1], out[2], out[3]]);
        let status = i32::from_le_bytes([out[4], out[5], out[6], out[7]]);
        if success == 0 {
            unsafe { CloseHandle(handle) };
            return Err(io::Error::new(
                io::ErrorKind::ConnectionRefused,
                format!("driver connect failed, NTSTATUS=0x{:08X}", status as u32),
            ));
        }

        let transport: Arc<dyn L2capTransport> = Arc::new(WindowsL2cap {
            handle: Arc::new(DriverHandle(handle)),
        });
        Ok(transport)
    })
    .await
    .map_err(|e| io::Error::new(io::ErrorKind::Other, e))?
}
