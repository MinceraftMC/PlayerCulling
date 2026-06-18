use core::arch::x86_64::__m128i;
use core::ops::{Add, Sub};
use std::fmt::Display;
use std::ops::{AddAssign, SubAssign};

#[repr(C)]
#[derive(Copy, Clone)]
pub struct Vec3i32Fields {
    pub x: i32,
    pub y: i32,
    pub z: i32,
    pub w: i32, // padding
}

#[repr(C)]
#[derive(Copy, Clone)]
pub union Vec3i32 {
    pub fields: Vec3i32Fields,
    pub vec: __m128i,
}

impl Vec3i32 {
    #[inline]
    pub fn new(x: i32, y: i32, z: i32) -> Self {
        Self {
            fields: Vec3i32Fields { x, y, z, w: 0 },
        }
    }

    #[inline]
    pub fn zero() -> Self {
        Self::new(0, 0, 0)
    }

    #[inline]
    pub fn from_m128i(vec: __m128i) -> Self {
        Self { vec }
    }

    #[inline]
    pub fn x(&self) -> i32 {
        unsafe { self.fields.x }
    }
    #[inline]
    pub fn y(&self) -> i32 {
        unsafe { self.fields.y }
    }
    #[inline]
    pub fn z(&self) -> i32 {
        unsafe { self.fields.z }
    }
}

impl Add for Vec3i32 {
    type Output = Self;

    #[inline]
    fn add(self, rhs: Self) -> Self {
        Self {
            fields: Vec3i32Fields {
                x: self.x() + rhs.x(),
                y: self.x() + rhs.y(),
                z: self.x() + rhs.z(),
                w: 0,
            },
        }
    }
}
impl AddAssign for Vec3i32 {
    #[inline]
    fn add_assign(&mut self, rhs: Self) {
        unsafe {
            self.fields.x += rhs.x();
            self.fields.y += rhs.y();
            self.fields.z += rhs.z();
        }
    }
}

impl Add<i32> for Vec3i32 {
    type Output = Self;

    #[inline]
    fn add(self, scalar: i32) -> Self {
        Self {
            fields: Vec3i32Fields {
                x: self.x() + scalar,
                y: self.y() + scalar,
                z: self.z() + scalar,
                w: 0,
            },
        }
    }
}
impl AddAssign<i32> for Vec3i32 {
    #[inline]
    fn add_assign(&mut self, rhs: i32) {
        unsafe {
            self.fields.x += rhs;
            self.fields.y += rhs;
            self.fields.z += rhs;
        }
    }
}

impl Sub for Vec3i32 {
    type Output = Self;

    #[inline]
    fn sub(self, rhs: Self) -> Self {
        Self {
            fields: Vec3i32Fields {
                x: self.x() - rhs.x(),
                y: self.y() - rhs.y(),
                z: self.z() - rhs.z(),
                w: 0,
            },
        }
    }
}
impl SubAssign for Vec3i32 {
    #[inline]
    fn sub_assign(&mut self, rhs: Self) {
        unsafe {
            self.fields.x -= rhs.x();
            self.fields.y -= rhs.y();
            self.fields.z -= rhs.z();
        }
    }
}

impl Display for Vec3i32 {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", unsafe { self.fields.to_string() })
    }
}

impl Display for Vec3i32Fields {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "Vec3i32[{},{},{}]", self.x, self.y, self.z)
    }
}
