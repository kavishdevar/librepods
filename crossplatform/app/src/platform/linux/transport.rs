use crate::platform::{DeviceId, L2capTransport};
use bluer::AddressType;
use bluer::l2cap::{SeqPacket, Socket, SocketAddr};
use std::io;
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::time::sleep;

const POLL_INTERVAL: Duration = Duration::from_millis(200);

/// Linux L2CAP transport backed by a BlueZ `SeqPacket` socket.
pub struct LinuxL2cap {
    sp: SeqPacket,
}

#[async_trait::async_trait]
impl L2capTransport for LinuxL2cap {
    async fn send(&self, data: &[u8]) -> io::Result<usize> {
        self.sp.send(data).await
    }

    async fn recv(&self, buf: &mut [u8]) -> io::Result<usize> {
        self.sp.recv(buf).await
    }
}

/// Open an L2CAP channel to `addr` on `psm` (BR/EDR), waiting up to `timeout`
/// for the connection to be fully established (peer CID assigned).
pub async fn l2cap_connect(
    addr: DeviceId,
    psm: u16,
    timeout: Duration,
) -> io::Result<Arc<dyn L2capTransport>> {
    let sa = SocketAddr::new(addr, AddressType::BrEdr, psm);
    let socket = Socket::new_seq_packet()?;
    let sp = tokio::time::timeout(timeout, socket.connect(sa))
        .await
        .map_err(|_| io::Error::new(io::ErrorKind::TimedOut, "L2CAP connect timed out"))??;

    let start = Instant::now();
    loop {
        match sp.peer_addr() {
            Ok(peer) if peer.cid != 0 => break,
            Ok(_) => { /* still establishing */ }
            Err(e) if e.raw_os_error() == Some(107) => {
                return Err(io::Error::new(
                    io::ErrorKind::NotConnected,
                    "peer disconnected during connection setup",
                ));
            }
            Err(_) => { /* transient */ }
        }
        if start.elapsed() >= timeout {
            return Err(io::Error::new(
                io::ErrorKind::TimedOut,
                "timed out establishing L2CAP channel",
            ));
        }
        sleep(POLL_INTERVAL).await;
    }

    Ok(Arc::new(LinuxL2cap { sp }))
}
