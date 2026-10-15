//! SYNTHETIC BRIDGE FIXTURE — NOT ELDEN RING GAME DATA.
use super::data::*;
pub const WALK_SPEED:f32=2.0;
pub const RUN_SPEED:f32=4.2;
pub const RUN_BACK_SPEED:f32=3.2;
pub const RUN_SIDE_SPEED:f32=3.5;
pub const SPRINT_SPEED:f32=6.5;
pub const CROUCH_WALK_SPEED:f32=1.0;
pub const CROUCH_RUN_SPEED:f32=2.0;
pub const MAX_HP:f32=100.;
pub const MAX_STAMINA:f32=100.;
pub const DEFAULT_WEAPON:usize=0;
pub const FIST:usize=0;
pub const TORCH:usize=1;
pub const SHIELD:usize=2;
pub const WEAPONS:&[WeaponInfo]=&[
WeaponInfo{name:"Fixture",category:0,attack:1.,weight:1.,stance:[0,0]},
WeaponInfo{name:"Fixture torch",category:0,attack:1.,weight:1.,stance:[0,0]},
WeaponInfo{name:"Fixture shield",category:0,attack:1.,weight:1.,stance:[0,0]},
];
const ROLL:&[[f32;3]]=&[[0.000000,0.000000,0.000000],[0.000000,0.000000,0.372288],[0.000000,0.000000,0.719921],[0.000000,0.000000,1.042899],[0.000000,0.000000,1.341223],[0.000000,0.000000,1.614892],[0.000000,0.000000,1.863905],[0.000000,0.000000,2.088264],[0.000000,0.000000,2.287968],[0.000000,0.000000,2.463018],[0.000000,0.000000,2.613412],[0.000000,0.000000,2.739152],[0.000000,0.000000,2.840237],[0.000000,0.000000,2.916667],[0.000000,0.000000,2.968442],[0.000000,0.000000,2.995562],[0.000000,0.000000,3.000000],[0.000000,0.000000,3.000000],[0.000000,0.000000,3.000000],[0.000000,0.000000,3.000000],[0.000000,0.000000,3.000000],[0.000000,0.000000,3.000000],[0.000000,0.000000,3.000000],[0.000000,0.000000,3.000000],[0.000000,0.000000,3.000000]];
const BACK:&[[f32;3]]=&[[0.000000,0.000000,-0.000000],[0.000000,0.000000,-0.245453],[0.000000,0.000000,-0.468990],[0.000000,0.000000,-0.670611],[0.000000,0.000000,-0.850318],[0.000000,0.000000,-1.008109],[0.000000,0.000000,-1.143984],[0.000000,0.000000,-1.257944],[0.000000,0.000000,-1.349989],[0.000000,0.000000,-1.420118],[0.000000,0.000000,-1.468332],[0.000000,0.000000,-1.494631],[0.000000,0.000000,-1.500000],[0.000000,0.000000,-1.500000],[0.000000,0.000000,-1.500000],[0.000000,0.000000,-1.500000],[0.000000,0.000000,-1.500000],[0.000000,0.000000,-1.500000],[0.000000,0.000000,-1.500000]];
pub fn base(id:ActionId)->Option<ActionDef> {
let (total,motion)=match id {
ActionId::Roll(..)|ActionId::CrouchRoll(..)=>(24.,ROLL),
ActionId::Backstep=>(18.,BACK),
ActionId::Attack(..)=>return None,
_=>(10.,&[][..])};
Some(ActionDef{name:"BRIDGE FIXTURE",source:"fixture",total,input_from:total-4.,input_dodge_from:total-4.,
cancel_light:total,cancel_heavy:total,cancel_dodge:total,cancel_jump:total,cancel_guard:total,
cancel_move:total,cancel_left:total,iframes:(0.,0.),jump_frames:false,stamina:0.,hits:&[],
charge:None,no_turn:&[(0.,100.)],turn:&[],motion})
}
pub fn attack(_:usize,_:bool,_:AttackKind)->Option<ActionDef>{None}
pub fn air_attack(_:usize,_:bool,_:bool)->Option<AirAttackDef>{None}
pub fn air_paired(_:usize)->Option<AirAttackDef>{None}
pub fn swap(_:SwapKind)->SwapDef{SwapDef{start:"fixture",end:"fixture",start_len:1.,end_len:1.,apply:1.,free_from:1.}}

pub fn air_windows(_weapon: usize, _two_hand: bool, _kind: u8) -> &'static [Hit] { &[] }
