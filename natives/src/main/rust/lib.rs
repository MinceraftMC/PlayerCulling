use crate::vector::f64vec3::Vec3f64;
use crate::vector::i32vec3::Vec3i32;
use microbench::Options;
use std::ops::AddAssign;

mod vector;

fn main() {
    let vec = Vec3i32::new(1, 2, 3);
    // let vec2 = Vec3f64::new(1, 2, 3);
    // println!("{} {} {}", vec, vec.len(), vec.distance_squared(vec));

    let opts = Options::default();
    microbench::bench(&opts, "add_assign", || {
        let mut v = Vec3i32::zero();
        v += vec;
        return v;
    });
}
