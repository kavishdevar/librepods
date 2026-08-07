//! Windows: an IPC client of `librepodsd` (Phase 3 — the "web-app" model).
//!
//! The daemon owns the single AAP session; the full app renders from the daemon's
//! `Snapshot` state and sends `Command`s, instead of running its own session over
//! the exclusive driver. This module is the transport for that: an async client
//! of the daemon's two named pipes (events in, commands out). It is additive —
//! wiring it into the GUI in place of the live session is done incrementally.

use std::time::Duration;

use librepods_ipc::{from_line, to_line, Command, Event, Snapshot, PIPE_CMDS, PIPE_EVENTS};
use tokio::io::{AsyncBufReadExt, AsyncWriteExt, BufReader};
use tokio::net::windows::named_pipe::ClientOptions;
use tokio::sync::mpsc;

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
