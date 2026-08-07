//! Windows: an IPC client of `librepodsd` (Phase 3 — the "web-app" model).
//!
//! The daemon owns the single AAP session; the full app renders from the daemon's
//! `Snapshot` state and sends `Command`s, instead of running its own session over
//! the exclusive driver. This module is the transport for that: an async client
//! of the daemon's two named pipes (events in, commands out). It is additive —
//! wiring it into the GUI in place of the live session is done incrementally.

use std::io;
use std::time::Duration;

use librepods_ipc::{
    from_line, to_line, Command, Event, Snapshot, PIPE_CMDS, PIPE_EVENTS, PIPE_L2CAP_RX,
    PIPE_L2CAP_TX,
};
use tokio::io::{AsyncBufReadExt, AsyncReadExt, AsyncWriteExt, BufReader};
use tokio::net::windows::named_pipe::{ClientOptions, NamedPipeClient};
use tokio::sync::{mpsc, Mutex};

use crate::platform::L2capTransport;

/// Subscribe to the daemon's state: streams `Snapshot`s, reconnecting on drop.
pub fn subscribe_state() -> mpsc::UnboundedReceiver<Snapshot> {
    let (tx, rx) = mpsc::unbounded_channel();
    tokio::spawn(async move {
        loop {
            if let Ok(client) = ClientOptions::new().open(PIPE_EVENTS) {
                let mut lines = BufReader::new(client).lines();
                while let Ok(Some(line)) = lines.next_line().await {
                    match from_line::<Event>(&line) {
                        Some(Event::State(s)) => {
                            if tx.send(s).is_err() {
                                return; // receiver dropped
                            }
                        }
                        // Overlay / ConnectPrompt are the tray's job for now.
                        _ => {}
                    }
                }
            }
            tokio::time::sleep(Duration::from_millis(500)).await; // reconnect
        }
    });
    rx
}

/// A handle for sending commands to the daemon (never blocks the caller).
#[derive(Clone)]
pub struct DaemonCommands(mpsc::UnboundedSender<Command>);

impl DaemonCommands {
    pub fn send(&self, cmd: Command) {
        let _ = self.0.send(cmd);
    }
}

/// Start the command channel: queued `Command`s are written to the daemon's
/// command pipe by a background task (reconnecting on drop).
pub fn command_sender() -> DaemonCommands {
    let (tx, mut rx) = mpsc::unbounded_channel::<Command>();
    tokio::spawn(async move {
        loop {
            if let Ok(mut client) = ClientOptions::new().open(PIPE_CMDS) {
                while let Some(cmd) = rx.recv().await {
                    if client.write_all(to_line(&cmd).as_bytes()).await.is_err() {
                        break; // reconnect
                    }
                }
            }
            tokio::time::sleep(Duration::from_millis(500)).await;
        }
    });
    DaemonCommands(tx)
}

/// An `L2capTransport` that proxies through the daemon (approach B): the daemon
/// owns the exclusive driver, so the app's AAP session runs over these two pipes
/// (TX app→daemon→driver, RX daemon→app). Packets are `[u16 LE len][bytes]`.
pub struct DaemonL2cap {
    tx: Mutex<NamedPipeClient>,
    rx: Mutex<NamedPipeClient>,
}

impl DaemonL2cap {
    /// Connect to the daemon's L2CAP-proxy pipes, and ask it to open the session
    /// (opening the app = the user wants to use the AirPods). Fails if the daemon
    /// isn't running (the caller then falls back to the driver directly).
    pub async fn connect() -> io::Result<Self> {
        let tx = ClientOptions::new().open(PIPE_L2CAP_TX)?;
        let rx = ClientOptions::new().open(PIPE_L2CAP_RX)?;
        // Lift the daemon's connect gate (best-effort).
        if let Ok(mut cmds) = ClientOptions::new().open(PIPE_CMDS) {
            let _ = cmds.write_all(to_line(&Command::Connect).as_bytes()).await;
        }
        Ok(Self { tx: Mutex::new(tx), rx: Mutex::new(rx) })
    }
}

#[async_trait::async_trait]
impl L2capTransport for DaemonL2cap {
    async fn send(&self, data: &[u8]) -> io::Result<usize> {
        let mut frame = Vec::with_capacity(data.len() + 2);
        frame.extend_from_slice(&(data.len() as u16).to_le_bytes());
        frame.extend_from_slice(data);
        self.tx.lock().await.write_all(&frame).await?;
        Ok(data.len())
    }

    async fn recv(&self, buf: &mut [u8]) -> io::Result<usize> {
        let mut rx = self.rx.lock().await;
        let mut len_buf = [0u8; 2];
        rx.read_exact(&mut len_buf).await?;
        let len = u16::from_le_bytes(len_buf) as usize;
        let mut packet = vec![0u8; len];
        rx.read_exact(&mut packet).await?;
        let n = len.min(buf.len());
        buf[..n].copy_from_slice(&packet[..n]);
        Ok(n)
    }
}
