use std::arch::x86_64::__m256d;
use std::fmt::Display;
use std::ops::{Add, AddAssign, Div, DivAssign, Mul, MulAssign, Sub, SubAssign};

#[repr(C)]
#[derive(Copy, Clone)]
pub struct Vec3f64Fields {
    pub x: f64,
    pub y: f64,
    pub z: f64,
    pub w: f64, // padding
}

#[repr(C)]
#[derive(Copy, Clone)]
pub union Vec3f64 {
    pub fields: Vec3f64Fields,
    pub vec: __m256d,
}

impl Vec3f64 {
    #[inline]
    pub fn new(x: f64, y: f64, z: f64) -> Self {
        Self {
            fields: Vec3f64Fields { x, y, z, w: 0.0 },
        }
    }

    #[inline]
    pub fn zero() -> Self {
        Self::new(0.0, 0.0, 0.0)
    }

    #[inline]
    pub fn from_m256d(vec: __m256d) -> Self {
        Self { vec }
    }

    #[inline]
    pub fn len_squared(&self) -> f64 {
        self.x() * self.x() + self.y() * self.y() + self.z() * self.z()
    }
    #[inline]
    pub fn len(&self) -> f64 {
        self.len_squared().sqrt()
    }

    #[inline]
    pub fn distance_squared(&self, other: Self) -> f64 {
        let dx = self.x() - other.x();
        let dy = self.y() - other.y();
        let dz = self.z() - other.z();
        dx * dx + dy * dy + dz * dz
    }

    #[inline]
    pub fn x(&self) -> f64 {
        unsafe { self.fields.x }
    }
    #[inline]
    pub fn y(&self) -> f64 {
        unsafe { self.fields.y }
    }
    #[inline]
    pub fn z(&self) -> f64 {
        unsafe { self.fields.z }
    }
}

impl Add for Vec3f64 {
    type Output = Self;

    #[inline]
    fn add(self, rhs: Self) -> Self::Output {
        Self {
            fields: Vec3f64Fields {
                x: self.x() + rhs.x(),
                y: self.y() + rhs.y(),
                z: self.z() + rhs.z(),
                w: 0.0,
            },
        }
    }
}
impl AddAssign for Vec3f64 {
    #[inline]
    fn add_assign(&mut self, rhs: Self) {
        unsafe {
            self.fields.x += rhs.x();
            self.fields.y += rhs.y();
            self.fields.z += rhs.z();
        }
    }
}

impl Add<f64> for Vec3f64 {
    type Output = Self;

    #[inline]
    fn add(self, rhs: f64) -> Self::Output {
        Self {
            fields: Vec3f64Fields {
                x: self.x() + rhs,
                y: self.y() + rhs,
                z: self.z() + rhs,
                w: 0.0,
            },
        }
    }
}
impl AddAssign<f64> for Vec3f64 {
    #[inline]
    fn add_assign(&mut self, rhs: f64) {
        unsafe {
            self.fields.x += rhs;
            self.fields.y += rhs;
            self.fields.z += rhs;
        }
    }
}

impl Sub for Vec3f64 {
    type Output = Self;

    #[inline]
    fn sub(self, rhs: Self) -> Self::Output {
        Self {
            fields: Vec3f64Fields {
                x: self.x() - rhs.x(),
                y: self.y() - rhs.y(),
                z: self.z() - rhs.z(),
                w: 0.0,
            },
        }
    }
}
impl SubAssign for Vec3f64 {
    #[inline]
    fn sub_assign(&mut self, rhs: Self) {
        unsafe {
            self.fields.x -= rhs.x();
            self.fields.y -= rhs.y();
            self.fields.z -= rhs.z();
        }
    }
}

impl Mul for Vec3f64 {
    type Output = Self;

    #[inline]
    fn mul(self, rhs: Self) -> Self::Output {
        Self {
            fields: Vec3f64Fields {
                x: self.x() * rhs.x(),
                y: self.y() * rhs.y(),
                z: self.z() * rhs.z(),
                w: 0.0,
            },
        }
    }
}
impl MulAssign for Vec3f64 {
    #[inline]
    fn mul_assign(&mut self, rhs: Self) {
        unsafe {
            self.fields.x *= rhs.x();
            self.fields.y *= rhs.y();
            self.fields.z *= rhs.z();
        }
    }
}

impl Div for Vec3f64 {
    type Output = Self;

    #[inline]
    fn div(self, rhs: Self) -> Self::Output {
        Self {
            fields: Vec3f64Fields {
                x: self.x() / rhs.x(),
                y: self.y() / rhs.y(),
                z: self.z() / rhs.z(),
                w: 0.0,
            },
        }
    }
}
impl DivAssign for Vec3f64 {
    #[inline]
    fn div_assign(&mut self, rhs: Self) {
        unsafe {
            self.fields.x /= rhs.x();
            self.fields.y /= rhs.y();
            self.fields.z /= rhs.z();
        }
    }
}

impl Display for Vec3f64 {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", unsafe { self.fields.to_string() })
    }
}

impl Display for Vec3f64Fields {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "Vec3f64[{},{},{}]", self.x, self.y, self.z)
    }
}
