use crate::vector::i32vec3::Vec3i32;
use std::ops::{Add, AddAssign};

mod vector;

fn main() {
    let mut vec = Vec3i32::zero();
    let vec2 = Vec3i32::new(1, 2, 3);
    vec += vec2;
    vec += vec2;
    vec += vec2;
    vec -= vec2;
    println!("{}", vec + 4)
}
