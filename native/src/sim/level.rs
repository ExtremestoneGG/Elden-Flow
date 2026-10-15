//! Horizontal intent only. Minecraft owns collision, gravity, steps and falls.
//! Input is allowed only on solid ground in this movement prototype.
use glam::Vec3;
pub struct Level { pub floor: f32 }
impl Level {
    pub fn height(&self, _x: f32, _z: f32) -> f32 { self.floor }
    pub fn slide(&self, pos: &mut Vec3, delta: Vec3) { *pos += delta; }
}
