use std::arch::x86_64::{__m256i, _mm256_add_epi32, _mm256_blendv_epi8};
use std::ops::{Add, AddAssign};

#[repr(C)]
#[derive(Copy, Clone)]
pub union Vec3i32x8Component {
    pub v: [i32; 8],
    pub vec: __m256i,
}

impl Vec3i32x8Component {
    #[inline]
    pub fn new(v: [i32; 8]) -> Self {
        Self { v }
    }
    #[inline]
    pub fn zero() -> Self {
        Self { v: [0; 8] }
    }
}

impl Add for Vec3i32x8Component {
    type Output = Self;

    #[inline]
    fn add(self, rhs: Self) -> Self::Output {
        unsafe {
            Self {
                vec: _mm256_add_epi32(self.vec, rhs.vec),
            }
        }
    }
}
impl AddAssign for Vec3i32x8Component {
    #[inline]
    fn add_assign(&mut self, rhs: Self) {
        unsafe {
            self.vec = _mm256_add_epi32(self.vec, rhs.vec);
        }
    }
}

#[repr(C)]
#[derive(Copy, Clone)]
pub struct Vec3i32x8 {
    pub x: Vec3i32x8Component,
    pub y: Vec3i32x8Component,
    pub z: Vec3i32x8Component,
}

impl Vec3i32x8 {
    #[inline]
    pub fn new(x: [i32; 8], y: [i32; 8], z: [i32; 8]) -> Self {
        Self {
            x: Vec3i32x8Component::new(x),
            y: Vec3i32x8Component::new(y),
            z: Vec3i32x8Component::new(z),
        }
    }

    #[inline]
    pub fn zero() -> Self {
        Self {
            x: Vec3i32x8Component::zero(),
            y: Vec3i32x8Component::zero(),
            z: Vec3i32x8Component::zero(),
        }
    }

    #[inline]
    pub fn blendv(&mut self, other: Self, mask: __m256i) {
        unsafe {
            self.x.vec = _mm256_blendv_epi8(self.x.vec, other.x.vec, mask);
            self.y.vec = _mm256_blendv_epi8(self.y.vec, other.y.vec, mask);
            self.z.vec = _mm256_blendv_epi8(self.z.vec, other.z.vec, mask);
        }
    }
}

impl Add for Vec3i32x8 {
    type Output = Self;

    #[inline]
    fn add(self, rhs: Self) -> Self::Output {
        Self {
            x: self.x + rhs.x,
            y: self.y + rhs.y,
            z: self.z + rhs.z,
        }
    }
}
impl AddAssign for Vec3i32x8 {
    #[inline]
    fn add_assign(&mut self, rhs: Self) {
        self.x += rhs.x;
        self.y += rhs.y;
        self.z += rhs.z;
    }
}
