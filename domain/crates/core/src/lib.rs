/// Core domain models and business logic for cat facts API
/// 
/// This crate contains pure business logic with no I/O dependencies,
/// making it easy to test and reuse across platforms.

mod models;
mod service;

pub use models::*;
pub use service::*;
