//! Adapted from Funny-Bones/ELDEN-RING-Combat-Rewrite (MIT).
//! Only vector math remains; no Bevy renderer, window or ECS.
use std::f32::consts::{PI, TAU};
use glam::Vec3;
pub mod data;
pub mod player;
pub mod level;
#[cfg(feature = "game-data")]
pub mod extracted;
#[cfg(not(feature = "game-data"))]
#[path = "fixture.rs"]
pub mod extracted;
pub fn dir_of(yaw: f32) -> Vec3 { Vec3::new(yaw.sin(), 0.0, yaw.cos()) }
pub fn yaw_of(dir: Vec3) -> f32 { dir.x.atan2(dir.z) }
pub fn angle_diff(from: f32, to: f32) -> f32 { (to-from+PI).rem_euclid(TAU)-PI }
pub fn turn_toward(yaw: f32, goal: f32, max_step: f32) -> f32 { yaw+angle_diff(yaw,goal).clamp(-max_step,max_step) }
pub fn approach(value: f32, goal: f32, max_step: f32) -> f32 { value+(goal-value).clamp(-max_step,max_step) }
