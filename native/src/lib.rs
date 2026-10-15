//! C ABI for a small Java/JNA bridge. All gameplay transitions are upstream.
#![allow(dead_code)]
mod sim;
use glam::{Vec2, Vec3};
use sim::player::{Button, Input, Player, State};
use sim::data::ActionId;
use std::panic::{catch_unwind, AssertUnwindSafe};

pub struct Controller { player: Player, held: bool, jump_held: bool, heavy_held: bool, left_held: bool, serial: u32, blade: [f32;107], locked: bool, window: i32, action_serial: u32, contacts: [f32; 961] }
impl Controller {fn new()->Self {Self{player:Player::new(Vec3::ZERO),held:false,jump_held:false,heavy_held:false,left_held:false,serial:0,blade:[0.;107],locked:false,window:-1,action_serial:0,contacts:[0.;961]}}}
// Contacts carry a stable action/window identity and the actual striking hand.
#[no_mangle]
pub unsafe extern "C" fn es_contacts(handle:*const Controller,output:*mut f32,len:u32)->i32 {
    if handle.is_null()||output.is_null()||len!=961{return -1;}
    std::slice::from_raw_parts_mut(output,961).copy_from_slice(&(*handle).contacts);0
}
#[no_mangle]
pub unsafe extern "C" fn es_weight(handle:*mut Controller,weight:f32)->i32 {
    if handle.is_null()||!weight.is_finite()||weight<=0.||weight>100.{return -1;}
    (*handle).player.load=if weight<=4.5{sim::data::Load::Light}else if weight<=8.{sim::data::Load::Medium}else{sim::data::Load::Heavy};0
}
#[no_mangle]
pub unsafe extern "C" fn es_impact(handle:*mut Controller){
    if !handle.is_null(){(*handle).player.hit_stop=0.025;}
}
#[no_mangle]
pub unsafe extern "C" fn es_hit_hand(handle:*mut Controller,flags:u32){
    if !handle.is_null(){es_impact(handle);if let Some(a)=&mut (*handle).player.air_attack{a.hit_hands|=1<<(flags&1);}}
}
#[no_mangle]
pub unsafe extern "C" fn es_spend(handle:*mut Controller,amount:f32){
    if !handle.is_null()&&amount.is_finite()&&amount>0.{let p=&mut (*handle).player;p.stamina=(p.stamina-amount).max(0.);}
}
/// Apply a server reconciliation delta or validated refund; clamps both ends.
#[no_mangle]
pub unsafe extern "C" fn es_adjust_stamina(handle:*mut Controller,delta:f32){
    if !handle.is_null()&&delta.is_finite(){let p=&mut (*handle).player;p.stamina=(p.stamina+delta).clamp(0.,p.max_stamina);}
}
#[no_mangle]
pub unsafe extern "C" fn es_capacity(handle:*mut Controller,maximum:f32){
    if !handle.is_null()&&maximum.is_finite() {
        let p=&mut (*handle).player;
        p.max_stamina=maximum.clamp(sim::data::MAX_STAMINA,sim::data::MAX_STAMINA+10000.);
        p.stamina=p.stamina.min(p.max_stamina);
    }
}
fn clip_id(source:&str)->u32 {
    source.split_once('_').map(|(a,b)|a.trim_start_matches('a').parse::<u32>().unwrap_or(0)*1_000_000+b.parse::<u32>().unwrap_or(0)).unwrap_or(0)
}

#[no_mangle]
pub extern "C" fn es_abi_version() -> u32 { 1 }
#[no_mangle]
pub extern "C" fn es_has_game_data() -> u32 { cfg!(feature="game-data") as u32 }
#[no_mangle]
pub unsafe extern "C" fn es_crouch(handle:*mut Controller){
    if !handle.is_null() {let p=&mut (*handle).player;if matches!(p.state,State::Ground){p.crouching=!p.crouching;}}
}
/// Exact authored action clip, or the free locomotion clip selected by speed.
#[no_mangle]
pub unsafe extern "C" fn es_animation(handle: *const Controller) -> u32 {
    if handle.is_null() { return 0; }
    let p=&(*handle).player;
    if let Some(a)=p.air_attack {return clip_id(a.def.source);}
    if let State::Act(a)=p.state {return clip_id(a.id.def().source);}
    if let State::Air(_)=p.state {return 202040;}
    let stance=p.moveset().info().stance[if p.grip==sim::data::Grip::OneHand{0}else{1}] as u32*1_000_000;
    if p.crouching {return if p.speed<0.05{300000}else if p.speed<=sim::data::CROUCH_WALK_SPEED*1.15{320000}else{320100};}
    if p.speed < 0.05 {return stance+if p.guard_up(){100}else{0};} if p.sprinting{return stance+20200;}
    let mut dir=0;
    if (*handle).locked {let f=p.facing();let d=p.move_dir.dot(f);dir=if d>0.707{0}else if d< -0.707{1}else if p.move_dir.dot(Vec3::new(f.z,0.,-f.x))>0.{2}else{3};}
    stance+(if p.speed <= sim::data::WALK_SPEED * 1.15 {20000} else {20100})+dir+if p.guard_up(){1000}else{0}
}
#[no_mangle]
pub extern "C" fn es_create() -> *mut Controller {
    Box::into_raw(Box::new(Controller::new()))
}
/// All five blade segments of each of this tick's three native steps.
#[no_mangle]
pub unsafe extern "C" fn es_blade(handle:*const Controller,output:*mut f32,len:u32)->i32 {
    if handle.is_null()||output.is_null()||len!=107{return -1;}
    std::slice::from_raw_parts_mut(output,107).copy_from_slice(&(*handle).blade);0
}
#[no_mangle]
pub unsafe extern "C" fn es_confirm_hit(handle:*mut Controller){
    if !handle.is_null(){(*handle).player.mark_hit();(*handle).player.hit_stop=0.04;}
}
#[no_mangle]
pub unsafe extern "C" fn es_equip(handle:*mut Controller,weapon:u32,two_hand:u32)->i32 {
    if handle.is_null() || weapon as usize>=sim::data::WEAPONS.len(){return -1;}
    let c=&mut *handle;let p=&mut c.player;
    let grip=match two_hand {2=>sim::data::Grip::TwoHandLeft,1=>sim::data::Grip::TwoHandRight,_=>sim::data::Grip::OneHand};
    if p.weapon!=weapon as usize || p.grip!=grip {
        p.weapon=weapon as usize;p.grip=grip;p.buffer=None;p.air_attack=None;
        if matches!(p.state,State::Act(a) if matches!(a.id,ActionId::Attack(..))){p.state=State::Ground;}
        c.serial=c.serial.wrapping_add(1);c.window=-1;c.blade.fill(0.);
    }
    let weight=p.moveset().info().weight;
    p.load=if weight<=4.5{sim::data::Load::Light}else if weight<=8.{sim::data::Load::Medium}else{sim::data::Load::Heavy};0
}
/// Offhand changes are atomic with respect to the current attack. Armor never affects load.
#[no_mangle]
pub unsafe extern "C" fn es_left(handle:*mut Controller,weapon:u32)->i32 {
    if handle.is_null()||weapon as usize>=sim::data::WEAPONS.len(){return -1;}
    let c=&mut *handle;let p=&mut c.player;
    if p.left!=weapon as usize {
        p.left=weapon as usize;p.buffer=None;p.air_attack=None;
        if matches!(p.state,State::Act(a) if matches!(a.id,ActionId::Attack(..))){p.state=State::Ground;}
        c.serial=c.serial.wrapping_add(1);c.window=-1;c.blade.fill(0.);c.left_held=false;
    }
    // Keep the main weapon's roll category: two light weapons must not become a heavy roll.
    0
}
#[no_mangle]
pub unsafe extern "C" fn es_defense(handle:*const Controller)->u32 {
    if handle.is_null(){return 0;}let p=&(*handle).player;
    let roll=matches!(p.state,State::Act(a) if matches!(a.id,ActionId::Roll(..)|ActionId::CrouchRoll(..)));
    ((roll&&p.invincible()) as u32) | ((p.airborne() as u32)<<1)
}
/// Handle must have been returned by es_create and not previously destroyed.
#[no_mangle]
pub unsafe extern "C" fn es_destroy(handle: *mut Controller) {
    if !handle.is_null() { drop(Box::from_raw(handle)); }
}
/// Input: right, forward, camera yaw radians, dodge held, walk held, grounded.
/// Output: dx,dz,facing,state,frame,duration,stamina,max_stamina,speed,sprint.
/// Exactly three upstream 60 Hz steps per Minecraft 20 Hz tick.
#[no_mangle]
pub unsafe extern "C" fn es_tick(handle: *mut Controller, input: *const f32, input_len: u32, output: *mut f32, output_len: u32) -> i32 {
    if handle.is_null() || input.is_null() || output.is_null() || !(input_len==6 || input_len==7 || input_len==16 || input_len==17 || input_len==18) || !(output_len==10||output_len==12) { return -1; }
    let input = std::slice::from_raw_parts(input,input_len as usize);
    let output = std::slice::from_raw_parts_mut(output,output_len as usize);
    if input.iter().any(|v| !v.is_finite()) { output.fill(0.0); return -2; }
    match catch_unwind(AssertUnwindSafe(|| tick(&mut *handle,input,output))) { Ok(())=>0, Err(_)=> {output.fill(0.0); -3} }
}
fn tick(c: &mut Controller, values: &[f32], out: &mut [f32]) {
    c.contacts.fill(0.);
    let held=values[3]>0.5;
    let extended=values.len()>=16;
    let jump=extended&&values[9]>0.5;
    let combat=extended&&values[8]>0.5;
    let heavy=combat&&values.get(16).is_some_and(|v|*v>0.5);
    let left=combat&&values.get(17).is_some_and(|v|*v>0.5);
    let click=combat&&values[7]>0.5;
    let y=if extended {values[10]}else{0.};
    let level=sim::level::Level {floor:if extended {values[11].min(y)}else{0.}};
    let target=if extended&&values[15]>0.5{Some(Vec3::new(values[13],y,values[14]))}else{None};
    // Lock-on input follows the target plane, not the lagging visual camera.
    // This remains tangent while the camera/body catch up after a sideways roll.
    let movement_yaw=target.filter(|t|t.x*t.x+t.z*t.z>0.0001).map_or(values[2],|t|t.x.atan2(t.z));
    c.locked=target.is_some();c.blade.fill(0.);c.blade[0]=c.serial as f32;
    let p=&mut c.player;
    p.pos=Vec3::new(0.,y,0.);
    let mut inp=Input { mv:Vec2::new(values[0],values[1]).clamp_length_max(1.0), cam_yaw:movement_yaw,
        dodge:Button {held,pressed:held&&!c.held,released:!held&&c.held}, walk:values[4]>0.5,
        sprint:values.get(6).is_some_and(|v|*v>0.5),
        light:Button{held:click,pressed:click,released:false},
        heavy:Button{held:heavy,pressed:heavy&&!c.heavy_held,released:!heavy&&c.heavy_held},
        guard:Button{held:left,pressed:left&&!c.left_held,released:!left&&c.left_held},
        jump:Button{held:jump,pressed:jump&&!c.jump_held,released:!jump&&c.jump_held},..Default::default() };
    c.held=held;
    c.jump_held=jump;c.heavy_held=heavy;c.left_held=left;
    if !combat {p.air_attack=None;if matches!(p.state,State::Act(a) if matches!(a.id,ActionId::Attack(..))){p.state=State::Ground;p.buffer=None;}}
    if (click||heavy||left)&&target.is_none()&&matches!(p.state,State::Ground){p.yaw=values[2];}
    if values[5]<0.5 {
        // Do not queue rolls on landings, in menus, while swimming or flying.
        p.state=State::Ground; p.speed=0.0; p.buffer=None;p.air_attack=None;
        c.held=false;c.jump_held=false;c.heavy_held=false;c.left_held=false;
        out.fill(0.);out[2]=p.yaw;out[6]=p.stamina;out[7]=p.max_stamina;return;
    }
    for _ in 0..3 {
        let before=match p.state{State::Act(a)=>Some((a.id,a.f)),_=>None};
        let air_before=p.air_attack;
        p.step(&inp,&level,target,target.is_some());
        let new_attack=match p.state{State::Act(a) if matches!(a.id,ActionId::Attack(..))=>before.is_none_or(|(id,f)|id!=a.id||a.f<f),_=>false};
        if new_attack||(air_before.is_none()&&p.air_attack.is_some()) {c.action_serial=(c.action_serial+1)%200_000;c.serial=c.serial.wrapping_add(1);c.window=-1;c.blade.fill(0.);c.blade[0]=c.serial as f32;}
        if combat {
            use sim::data::AttackKind::*;
            let (windows,frame,heavy,charged,landed)=if let Some(a)=p.air_attack {
                (sim::extracted::air_windows(a.moveset.weapon as usize,a.moveset.two_hand,if a.paired{2}else{a.heavy as u8}),a.f,a.heavy,false,0)
            }else if let State::Act(a)=p.state {
                let heavy=matches!(a.id,ActionId::Attack(_,Heavy1|Heavy2|Heavy1Charge|Heavy2Charge|RunHeavy|JumpHeavyLand|JumpHeavyLandShort|GuardCounter));
                (a.id.def().hits,a.f,heavy,matches!(a.id,ActionId::Attack(_,Heavy1Charge|Heavy2Charge)),a.landed)
            }else{(&[][..],0.,false,false,0)};
            for (window,h) in windows.iter().enumerate() {
                if frame<h.from||frame>=h.to||landed&(1<<window)!=0{continue;}
                if let Some(sweep)=p.sweep(h.blade,h.from,frame){for (a,b) in sweep {
                    let n=c.contacts[0] as usize;if n>=96{break;}
                    let at=1+n*10;
                    let left=h.left||p.grip==sim::data::Grip::TwoHandLeft;
                    c.contacts[at..at+10].copy_from_slice(&[(c.action_serial*64+window as u32) as f32,((charged as u32)*4+(heavy as u32)*2+left as u32) as f32,h.radius,a.x,a.y-y,a.z,b.x,b.y-y,b.z,frame]);
                    c.contacts[0]+=1.;
                }}
            }
        }
        if combat {if let Some(hit)=p.active_hit(){
            let window=if p.air_attack.is_some(){100}else if let State::Act(a)=p.state{a.id.def().hits.iter().position(|h|a.f>=h.from&&a.f<h.to).unwrap_or(0) as i32}else{-1};
            if window!=c.window{c.window=window;c.serial=c.serial.wrapping_add(1);c.blade.fill(0.);c.blade[0]=c.serial as f32;}
            for (a,b) in hit.sweep {let n=c.blade[1] as usize;if n>=15{break;}let at=2+n*7;
                c.blade[at..at+7].copy_from_slice(&[hit.radius,a.x,a.y-y,a.z,b.x,b.y-y,b.z]);c.blade[1]+=1.;}
        }}
        inp.guard.pressed=false;inp.guard.released=false;
        inp.dodge.pressed=false; inp.dodge.released=false;inp.light.pressed=false;inp.heavy.pressed=false;inp.heavy.released=false;inp.jump.pressed=false;inp.jump.released=false;
    }
    let (mut state,mut frame,mut total)=match p.state {
        State::Act(a) => (match a.id { ActionId::Roll(..)|ActionId::CrouchRoll(..)=>1.0,ActionId::Backstep=>2.0,ActionId::Attack(..)=>4.0,ActionId::Jump(..)=>5.0,_=>3.0 }, a.f,a.id.def().total),
        State::Air(a)=>(5.0,a.f,60.),
        _=>(0.0,0.0,1.0)
    };
    if let Some(a)=p.air_attack {state=6.;frame=a.f;total=a.def.to;}
    out[..10].copy_from_slice(&[p.pos.x,p.pos.z,p.yaw,state,frame,total,p.stamina,p.max_stamina,p.speed,if p.sprinting {1.0}else{0.0}]);
    if out.len()>=12{out[10]=p.pos.y-y;out[11]=if p.airborne(){1.}else{0.};}
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn stamina_reconciliation_is_bounded_and_isolated(){
        let mut a=new();let b=new();unsafe{es_adjust_stamina(&mut a,-200.);}
        assert_eq!(a.player.stamina,0.);assert!(b.player.stamina>0.);
        unsafe{es_adjust_stamina(&mut a,2000.);}assert_eq!(a.player.stamina,a.player.max_stamina);
        unsafe{es_adjust_stamina(&mut a,f32::NAN);}assert!(a.player.stamina.is_finite());
    }
    #[cfg(feature="game-data")]
    #[test] fn paired_air_exports_each_hand_and_unique_windows() {
        for weapon in [0,1,2,4,5,6,7,8,9,11,13,14,16,18] {
            let mut c=new();unsafe{es_equip(&mut c,weapon,0);es_left(&mut c,weapon);}
            let mut input=[0.;18];let mut out=[0.;12];input[5]=1.;input[8]=1.;input[10]=64.;input[11]=64.;input[9]=1.;
            let mut air=false;let mut hands=[false;2];let mut windows=std::collections::HashSet::new();
            for i in 0..100 {
                input[17]=if i==6{1.}else{0.};tick(&mut c,&input,&mut out);input[9]=0.;input[10]+=out[10];
                air|=c.player.air_attack.is_some_and(|a|a.paired);
                for n in 0..c.contacts[0] as usize {let at=1+n*10;let flags=c.contacts[at+1] as u32;hands[(flags as usize)&1]=true;if windows.insert(c.contacts[at] as u32){unsafe{es_hit_hand(&mut c,flags);}}}
            }
            assert!(air,"no paired air animation for {weapon}");
            assert!(hands[0]&&hands[1],"weapon {weapon} missing hand: {hands:?}; windows {windows:?}");
            assert!(windows.len()>=2);
        }
    }
    #[cfg(feature="game-data")]
    #[test] fn hit_then_roll_preserves_side_input_with_moving_target_and_lock_loss() {
        for side in [-1.,1.] {for lost in [false,true] {
            let mut c=new();let mut input=[0.;18];let mut out=[0.;12];
            input[5]=1.;input[8]=1.;input[10]=64.;input[11]=64.;input[14]=8.;input[15]=1.;input[7]=1.;
            tick(&mut c,&input,&mut out);input[7]=0.;
            for _ in 0..45{tick(&mut c,&input,&mut out);if c.contacts[0]>0.{break;}}
            input[0]=side;input[3]=1.;tick(&mut c,&input,&mut out);input[3]=0.;
            let mut rolled=false;let mut resumed=false;
            for i in 0..100 {
                if i==12&&lost {input[15]=0.;}
                if !lost {input[13]=(i as f32*0.025).sin()*2.;}
                tick(&mut c,&input,&mut out);rolled|=out[3]==1.;
                if rolled&&out[3]==0.&&out[8]>0.1 {
                    resumed=true;
                    let yaw=if input[15]>0.5{input[13].atan2(input[14])}else{input[2]};
                    assert!((out[0]*yaw.sin()+out[1]*yaw.cos()).abs()<0.015,"forward drift {out:?}");
                    assert!((-out[0]*yaw.cos()+out[1]*yaw.sin())*side>0.);
                }
            }
            assert!(rolled&&resumed,"attack cancel did not roll/resume");
        }}
    }
    #[cfg(feature="game-data")]
    #[test] fn left_two_hand_and_material_load_are_explicit() {
        let mut c=new();unsafe{es_left(&mut c,7);es_equip(&mut c,1,2);}
        assert_eq!(c.player.moveset().weapon,7);assert!(c.player.moveset().two_hand);assert!(!c.player.paired());
        unsafe{es_weight(&mut c,3.);}assert_eq!(c.player.load,sim::data::Load::Light);
        unsafe{es_weight(&mut c,6.3);}assert_eq!(c.player.load,sim::data::Load::Medium);
        assert_eq!(unsafe{es_weight(&mut c,f32::NAN)},-1);
    }
    #[cfg(feature="game-data")]
    #[test] fn locked_lateral_roll_resumes_lateral_sprint_without_forward_drift() {
        let mut c=new();let mut input=[0.;17];let mut out=[0.;12];
        input[0]=1.;input[5]=1.;input[10]=64.;input[11]=64.;input[12]=1.;input[14]=8.;input[15]=1.;
        input[3]=1.;tick(&mut c,&input,&mut out);input[3]=0.;tick(&mut c,&input,&mut out);
        assert_eq!(out[3],1.);input[3]=1.;
        let mut ground=false;
        for _ in 0..45 {
            tick(&mut c,&input,&mut out);
            if out[3]==0. {ground=true;assert!(out[0]<0.);assert!(out[1].abs()<0.015,"moving toward target after roll: {out:?}");}
        }
        assert!(ground);
    }
    #[cfg(feature="game-data")]
    #[test] fn lock_strafe_stays_tangent_despite_camera_lag_before_and_after_roll() {
        for side in [-1.,1.] {for weapon in [0,1,16] {
            let mut c=new();unsafe{es_equip(&mut c,weapon,0);}
            let mut input=[0.;18];let mut out=[0.;12];input[0]=side;input[2]=1.2;
            input[5]=1.;input[8]=1.;input[10]=64.;input[11]=64.;input[12]=1.;input[13]=6.;input[14]=8.;input[15]=1.;
            input[3]=1.;for _ in 0..20{tick(&mut c,&input,&mut out);}
            input[3]=0.;tick(&mut c,&input,&mut out);
            // A sprint release is not a dodge: press/release again for the actual roll.
            input[3]=1.;tick(&mut c,&input,&mut out);input[3]=0.;tick(&mut c,&input,&mut out);
            input[3]=1.;let mut ground=false;
            for _ in 0..45{
                tick(&mut c,&input,&mut out);
                if out[3]==0.&&out[8]>0.1{
                    ground=true;let radial=out[0]*0.6+out[1]*0.8;
                    assert!(radial.abs()<0.015,"camera steered toward target: {out:?}");
                    assert!((-out[0]*0.8+out[1]*0.6)*side>0.);
                }
            }
            assert!(ground);
        }}
    }
    #[cfg(feature="game-data")]
    #[test] fn paired_classes_use_authored_attacks_without_changing_roll_weight() {
        use sim::data::AttackKind::*;
        for weapon in [0,1,2,4,5,8,9,11,13,14,16,18] {
            let mut c=new();unsafe{es_equip(&mut c,weapon,0);}
            let load=c.player.load;unsafe{es_left(&mut c,weapon);}
            assert_eq!(load,c.player.load);assert!(c.player.paired());
            let mv=c.player.moveset();
            for kind in [PairedLight1,PairedLight2,PairedRun,PairedRoll,PairedBackstep,PairedJumpLand] {
                let def=mv.attack(kind).expect("Missing paired variant");
                assert!(!def.hits.is_empty(),"weapon {weapon} {kind:?}");
            }
            assert!(mv.air_paired().is_some());
            let mut input=[0.;18];let mut out=[0.;12];
            input[5]=1.;input[8]=1.;input[10]=64.;input[11]=64.;input[12]=1.;
            let mut hit=false;let mut pair=false;
            for i in 0..80 {
                input[17]=if i==0{1.}else{0.};tick(&mut c,&input,&mut out);
                pair|=matches!(c.player.state,State::Act(a) if matches!(a.id,ActionId::Attack(_,PairedLight1)));
                hit|=c.blade[1]>0.;
            }
            assert!(pair&&hit,"weapon {weapon}");
            unsafe{es_equip(&mut c,weapon,1);}assert!(!c.player.paired());
        }
    }
    #[cfg(feature="game-data")]
    #[test] fn every_selected_class_and_grip_has_its_own_light_and_heavy_hits() {
        for weapon in [0,1,2,4,5,8,9,11,13,14,16,18] {for grip in [0,1] {for heavy in [false,true] {
            let mut c=new();assert_eq!(unsafe{es_equip(&mut c,weapon,grip)},0);
            let expected=c.player.moveset().info().category as u32;
            let mut input=[0.;17];let mut out=[0.;12];input[5]=1.;input[8]=1.;input[10]=64.;input[11]=64.;input[12]=1.;
            let mut hit=false;let mut authored=false;
            for i in 0..150 {
                input[7]=if !heavy&&i==0{1.}else{0.};input[16]=if heavy&&i<100{1.}else{0.};
                tick(&mut c,&input,&mut out);hit|=c.blade[1]>0.;
                if out[3]==4. {assert_eq!(unsafe{es_animation(&c)}/1_000_000,expected,"weapon {weapon} grip {grip}");authored=true;}
            }
            assert!(hit&&authored,"weapon {weapon} grip {grip} heavy {heavy}");
        }}}
    }
    #[cfg(feature="game-data")]
    #[test] fn weapon_weight_selects_roll_and_iframes_end() {
        let mut distances=vec![];
        for weapon in [0,13,16] {
            let mut c=new();unsafe{es_equip(&mut c,weapon,0);}
            let mut input=[0.;17];let mut out=[0.;12];input[1]=1.;input[5]=1.;input[10]=64.;input[11]=64.;input[12]=1.;input[3]=1.;
            tick(&mut c,&input,&mut out);input[3]=0.;let mut invul=0;let mut distance=0.;
            for _ in 0..50{tick(&mut c,&input,&mut out);if unsafe{es_defense(&c)}&1!=0{invul+=1;}if out[3]==1.{distance+=out[1];}input[1]=0.;}
            assert!(invul>0&&invul<25);assert_eq!(unsafe{es_defense(&c)}&1,0);distances.push(distance);
        }
        assert!(distances[0]>distances[2],"light/medium/heavy roll {distances:?}");
    }
    #[cfg(feature="game-data")]
    #[test] fn heavy_release_and_charge_use_distinct_real_clips_and_hits() {
        for charged in [false,true] {
            let mut c=new();let mut out=[0.;12];let mut input=[0.;17];
            input[5]=1.;input[8]=1.;input[10]=64.;input[11]=64.;input[12]=1.;
            let mut hit=false;let mut released_clip=false;let mut charged_clip=false;let mut charged_contact=false;
            for i in 0..100 {
                input[16]=if i==0||(charged&&i<65){1.}else{0.};
                tick(&mut c,&input,&mut out);
                let clip=unsafe{es_animation(&c)};
                released_clip|=clip==23030505;charged_clip|=clip==23030500;
                for n in 0..c.contacts[0] as usize{charged_contact|=(c.contacts[1+n*10+1] as u32)&4!=0;}
                hit|=c.blade[1]>0.;assert!(out.iter().all(|x|x.is_finite()));
            }
            assert!(hit&&charged_clip);assert_eq!(released_clip,!charged);assert_eq!(charged_contact,charged,"full-charge contact flag");
        }
    }
    #[cfg(feature="game-data")]
    #[test] fn heavy_in_air_uses_authored_air_and_landing_clips() {
        let mut c=new();let mut out=[0.;12];let mut input=[0.;17];
        input[5]=1.;input[8]=1.;input[10]=64.;input[11]=64.;input[12]=1.;input[9]=1.;
        let mut air=false;let mut hit=false;
        for i in 0..100 {
            input[16]=if i>=6&&i<=12{1.}else{0.};tick(&mut c,&input,&mut out);
            input[9]=0.;input[10]+=out[10];
            air|=unsafe{es_animation(&c)}==23031230;hit|=c.blade[1]>0.;
        }
        assert!(air&&hit);
    }
    #[cfg(feature="game-data")]
    #[test] fn locked_sprint_jump_landing_keeps_held_side() {
        for side in [-1.,1.] {
            let mut c=new();let mut out=[0.;12];let mut input=[0.;18];
            input[0]=side;input[3]=1.;input[5]=1.;input[8]=1.;
            input[10]=64.;input[11]=64.;input[12]=1.;input[14]=10.;input[15]=1.;
            for _ in 0..15 {tick(&mut c,&input,&mut out);}
            input[9]=1.;let mut airborne=false;let mut landed=false;let mut checked=0;
            for _ in 0..65 {
                tick(&mut c,&input,&mut out);input[9]=0.;input[10]+=out[10];
                airborne|=input[10]>64.1;
                if airborne&&input[10]<=64.001 {landed=true;}
                if landed {
                    checked+=1;
                    assert!(out[0]*-side>0. && out[1].abs()<out[0].abs()*0.2,
                        "landing drift side={side}, displacement=({}, {})",out[0],out[1]);
                }
            }
            assert!(checked>10,"jump never completed");
        }
    }
    #[cfg(feature="game-data")]
    #[test] fn moving_guard_selects_directional_leg_clip() {
        for weapon in [0,1,2,4,5,6,7,8,9,11,13,14,16,18] {for grip in [0,1] {
            let mut c=new();unsafe{es_equip(&mut c,weapon,grip);es_left(&mut c,24);}
            let mut input=[0.;18];let mut out=[0.;12];input[0]=1.;input[5]=1.;input[8]=1.;input[14]=10.;input[15]=1.;input[17]=1.;
            for _ in 0..20 {tick(&mut c,&input,&mut out);}
            let clip=unsafe{es_animation(&c)}%1_000_000;
            assert!((21000..=21003).contains(&clip)||(21100..=21103).contains(&clip),"weapon={weapon}, grip={grip}, clip={clip}");
        }}
    }
    #[test] fn upgraded_stamina_regenerates_to_new_cap_without_free_refills() {
        let mut c=new();let mut out=[0.;10];unsafe{es_capacity(&mut c,sim::data::MAX_STAMINA+5.);es_spend(&mut c,20.);}
        let before=c.player.stamina;unsafe{es_capacity(&mut c,sim::data::MAX_STAMINA+5.);}assert_eq!(before,c.player.stamina);
        for _ in 0..160 {tick(&mut c,&[0.,0.,0.,0.,0.,1.],&mut out);}
        assert_eq!(out[7],sim::data::MAX_STAMINA+5.);assert_eq!(out[6],out[7]);
    }
    fn advance(c:&mut Controller, input:[f32;6])->[f32;10] {let mut o=[0.0;10];tick(c,&input,&mut o);o}
    fn new()->Controller {Controller::new()}
    #[test] fn roll_on_release_backstep_when_still() {
        let mut c=new(); assert_eq!(advance(&mut c,[0.,1.,0.,1.,0.,1.])[3],0.);
        assert_eq!(advance(&mut c,[0.,1.,0.,0.,0.,1.])[3],1.);
        let mut c=new();advance(&mut c,[0.,0.,0.,1.,0.,1.]);
        assert_eq!(advance(&mut c,[0.,0.,0.,0.,0.,1.])[3],2.);
    }
    #[test] fn holding_sprints_release_does_not_roll() {
        let mut c=new(); let mut o=[0.;10];
        for _ in 0..12 {o=advance(&mut c,[0.,1.,0.,1.,0.,1.]);} assert_eq!(o[9],1.);
        assert_ne!(advance(&mut c,[0.,1.,0.,0.,0.,1.])[3],1.);
    }
    #[test] fn no_air_roll_or_deferred_roll() {
        let mut c=new();advance(&mut c,[0.,1.,0.,1.,0.,0.]);
        assert_eq!(advance(&mut c,[0.,1.,0.,0.,0.,1.])[3],0.);
    }
    #[test] fn dedicated_sprint_does_not_dodge_on_short_release() {
        let mut c=new();let mut out=[0.;10];
        tick(&mut c,&[0.,1.,0.,0.,0.,1.,1.],&mut out);
        assert_eq!(out[9],1.);
        tick(&mut c,&[0.,1.,0.,0.,0.,1.,0.],&mut out);
        assert_eq!(out[3],0.);
        assert_eq!(out[9],0.);
    }
    #[test] fn walk_is_slower_than_run_and_sprint() {
        let mut speeds=[0.;3];
        for mode in 0..3 {
            let mut c=new();let mut out=[0.;10];
            for _ in 0..25 {tick(&mut c,&[0.,1.,0.,0.,if mode==0{1.}else{0.},1.,if mode==2{1.}else{0.}],&mut out);}
            speeds[mode]=out[8];
        }
        assert!(speeds[0]<speeds[1] && speeds[1]<speeds[2],"{speeds:?}");
    }
    #[test] fn camera_relative_motion_is_finite_and_bounded() {
        let mut c=new(); let mut total=Vec2::ZERO;
        for _ in 0..20 {let o=advance(&mut c,[0.,1.,std::f32::consts::FRAC_PI_2,0.,0.,1.]);assert!(o.iter().all(|v|v.is_finite()));total+=Vec2::new(o[0],o[1]);}
        assert!(total.x>1. && total.x<12.);assert!(total.y.abs()<0.2);
    }
    #[test] fn ffi_rejects_invalid_input() {unsafe{let c=es_create();let mut o=[0.;10];let i=[f32::NAN;6];assert_eq!(es_tick(c,i.as_ptr(),6,o.as_mut_ptr(),10),-2);assert_eq!(es_tick(c,i.as_ptr(),5,o.as_mut_ptr(),10),-1);es_destroy(c);}}
    #[cfg(feature="game-data")]
    #[test] fn attack_opens_real_blade_window_after_windup() {
        let mut c=new();let mut out=[0.;12];let mut input=[0.;16];input[5]=1.;input[8]=1.;input[10]=64.;input[11]=64.;input[12]=1.;input[7]=1.;
        tick(&mut c,&input,&mut out);assert_eq!(out[3],4.);assert_eq!(c.blade[1],0.);
        let mut live=false;
        for _ in 0..60{input[7]=0.;tick(&mut c,&input,&mut out);if c.blade[1]>0.{live=true;assert!(c.blade.iter().all(|x|x.is_finite()));assert!(c.blade[1]<=15.);}}
        assert!(live,"no authored hit samples");
    }
    #[cfg(feature="game-data")]
    #[test] fn authored_jump_rises_and_lands_on_minecraft_floor() {
        let mut c=new();let mut out=[0.;12];let mut input=[0.;16];input[5]=1.;input[10]=64.;input[11]=64.;input[12]=1.;input[9]=1.;
        let mut peak=64f32;
        for _ in 0..100 {tick(&mut c,&input,&mut out);input[9]=0.;input[10]+=out[10];peak=peak.max(input[10]);assert!(input[10]>=63.999);assert!(out[10].abs()<4.);}
        assert!(peak>64.5&&peak<68.,"peak {peak}");assert!((input[10]-64.).abs()<0.001);
    }
    #[cfg(feature="game-data")]
    #[test] fn light_attack_in_jump_has_air_clip_and_blade() {
        let mut c=new();let mut out=[0.;12];let mut input=[0.;16];input[5]=1.;input[8]=1.;input[10]=64.;input[11]=64.;input[12]=1.;input[9]=1.;
        let mut air=false;let mut hit=false;
        for i in 0..70 {input[7]=if i>=6&&i<=20&&i%3==0{1.}else{0.};tick(&mut c,&input,&mut out);input[9]=0.;input[10]+=out[10];air|=out[3]==6.;hit|=c.blade[1]>0.;}
        assert!(air&&hit,"air={air} hit={hit}");
    }
}
