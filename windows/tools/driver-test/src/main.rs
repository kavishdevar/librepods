//! Integration testers for the LibrePodsAAP driver.
//!
//! Run AFTER the driver is installed (Test Mode). Examples:
//!   lp-driver-test find
//!   lp-driver-test battery
//!   lp-driver-test anc transparency

mod aap;
mod bt;
mod driver;

use driver::Driver;
use std::time::{Duration, Instant};

fn usage() {
    eprintln!("lp-driver-test <command> [args]");
    eprintln!("  find                  locate + open the installed driver");
    eprintln!("  status                query connection state");
    eprintln!("  connect [MAC]         open L2CAP to AirPods (PSM 0x1001)");
    eprintln!("  disconnect");
    eprintln!("  battery [MAC]         connect + handshake + read battery");
    eprintln!("  anc <off|anc|transparency|adaptive> [MAC]");
    eprintln!("  send <hex...>         send raw bytes on the open channel");
    eprintln!("  recv [timeout_ms]     read one packet");
}

fn resolve_mac(arg: Option<&String>) -> Result<u64, String> {
    match arg {
        Some(s) => bt::parse_mac(s).ok_or_else(|| format!("invalid MAC: {s}")),
        None => bt::find_airpods().ok_or_else(|| "no paired AirPods found; pass a MAC".into()),
    }
}

fn handshake(d: &Driver) -> std::io::Result<()> {
    d.send(&aap::HANDSHAKE)?;
    std::thread::sleep(Duration::from_millis(300));
    d.send(&aap::SET_FEATURES)?;
    std::thread::sleep(Duration::from_millis(300));
    d.send(&aap::REQUEST_NOTIFS)?;
    Ok(())
}

fn connect(mac: u64) -> Result<Driver, String> {
    let d = Driver::open().map_err(|e| e.to_string())?;
    println!("Connecting to {} PSM 0x{:04X}...", bt::fmt_mac(mac), aap::PSM_AACP);
    let (ok, st) = d.connect(mac, aap::PSM_AACP).map_err(|e| e.to_string())?;
    if !ok {
        return Err(format!("connect failed, NTSTATUS=0x{:08X}", st as u32));
    }
    println!("✅ L2CAP connected");
    Ok(d)
}

fn parse_hex(tokens: &[String]) -> Result<Vec<u8>, String> {
    tokens
        .iter()
        .map(|t| {
            u8::from_str_radix(t.trim_start_matches("0x"), 16).map_err(|_| format!("bad hex: {t}"))
        })
        .collect()
}

fn run(cmd: &str, args: &[String]) -> Result<(), String> {
    match cmd {
        "find" => {
            Driver::open().map_err(|e| e.to_string())?;
            println!("✅ driver found and opened");
            Ok(())
        }
        "status" => {
            let d = Driver::open().map_err(|e| e.to_string())?;
            let (state, addr) = d.status().map_err(|e| e.to_string())?;
            let s = match state {
                0 => "Disconnected",
                1 => "Connecting",
                2 => "Connected",
                _ => "?",
            };
            println!("state={s} addr={}", bt::fmt_mac(addr));
            Ok(())
        }
        "connect" => {
            let mac = resolve_mac(args.first())?;
            connect(mac)?;
            Ok(())
        }
        "disconnect" => {
            let d = Driver::open().map_err(|e| e.to_string())?;
            d.disconnect().map_err(|e| e.to_string())?;
            println!("disconnected");
            Ok(())
        }
        "battery" => {
            let mac = resolve_mac(args.first())?;
            let d = connect(mac)?;
            println!("handshake...");
            handshake(&d).map_err(|e| e.to_string())?;
            println!("reading packets for ~10s...\n");
            let mut buf = [0u8; 1024];
            let start = Instant::now();
            let mut got = false;
            while start.elapsed() < Duration::from_secs(10) {
                if let Ok(n) = d.receive(1500, &mut buf) {
                    if n > 0 {
                        let data = &buf[..n];
                        println!("<< {}", aap::hex(data));
                        if aap::is_battery(data) {
                            aap::print_battery(&data[4..]);
                            got = true;
                        }
                    }
                }
            }
            if got {
                println!("\n🎉 battery received — end-to-end AAP over the driver works!");
            } else {
                println!("\n⚠️ connected + handshake sent, but no battery packet in 10s.");
            }
            Ok(())
        }
        "anc" => {
            let mode_s = args
                .first()
                .ok_or("usage: anc <off|anc|transparency|adaptive> [MAC]")?;
            let mode = aap::parse_anc_mode(mode_s).ok_or_else(|| format!("bad mode: {mode_s}"))?;
            let mac = resolve_mac(args.get(1))?;
            let d = connect(mac)?;
            handshake(&d).map_err(|e| e.to_string())?;
            std::thread::sleep(Duration::from_millis(300));
            d.send(&aap::anc_command(mode)).map_err(|e| e.to_string())?;
            println!("✅ sent ANC command '{mode_s}' (0x{mode:02X}) — listen for the change!");
            Ok(())
        }
        "send" => {
            if args.is_empty() {
                return Err("usage: send <hex bytes...>".into());
            }
            let bytes = parse_hex(args)?;
            let d = Driver::open().map_err(|e| e.to_string())?;
            d.send(&bytes).map_err(|e| e.to_string())?;
            println!(">> {}", aap::hex(&bytes));
            Ok(())
        }
        "recv" => {
            let to: u32 = args.first().and_then(|s| s.parse().ok()).unwrap_or(3000);
            let d = Driver::open().map_err(|e| e.to_string())?;
            let mut buf = [0u8; 1024];
            let n = d.receive(to, &mut buf).map_err(|e| e.to_string())?;
            println!("<< ({n} bytes) {}", aap::hex(&buf[..n]));
            Ok(())
        }
        _ => {
            usage();
            Err(format!("unknown command: {cmd}"))
        }
    }
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    if args.len() < 2 {
        usage();
        std::process::exit(2);
    }
    if let Err(e) = run(&args[1], &args[2..]) {
        eprintln!("❌ {e}");
        std::process::exit(1);
    }
}
