use crate::vector::i32vec3::Vec3i32;
use crate::vector::i32vec3x8::{Vec3i32x8, Vec3i32x8Component};
use microbench::Options;

mod vector;

fn main() {
    let vec = Vec3i32::new(1, 2, 3);
    // let vec2 = Vec3f64::new(1, 2, 3);
    // println!("{} {} {}", vec, vec.len(), vec.distance_squared(vec));

    let opts = Options::default();
    // microbench::bench(&opts, "zero_avx", || Vec3i32x8::zero_avx());
    microbench::bench(&opts, "zero", || Vec3i32x8::zero());
    // microbench::bench(&opts, "component_zero_avx", || Vec3i32x8Component::zero_avx());
    microbench::bench(&opts, "component_zero", || Vec3i32x8Component::zero());
}
