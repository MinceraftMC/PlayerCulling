//
// Created by pianoman911 on 02.06.26.
//

#ifndef NATIVES_VECTOR_H
#define NATIVES_VECTOR_H

#include <cinttypes>
#include <cmath>
#include <immintrin.h>

#if defined(__GNUC__) || defined(__clang__)
#define FORCE_INLINE __attribute__((always_inline)) inline
#else
#define FORCE_INLINE inline
#endif

class i3_vec32 {
public:
    i3_vec32(int32_t x, int32_t y, int32_t z);

    explicit i3_vec32(__m128i vec);

    i3_vec32() {
        x = 0;
        y = 0;
        z = 0;
    }

    i3_vec32 operator+(const i3_vec32 &other) const {
        return i3_vec32(_mm_add_epi32(vec, other.vec));
    }

    i3_vec32 operator+(const double scalar) const {
        __m128i scalar_vec = _mm_set1_epi32(scalar);
        return i3_vec32(_mm_add_epi32(scalar_vec, vec));
    }

    i3_vec32 operator-(const i3_vec32 &other) const {
        return i3_vec32(_mm_sub_epi32(vec, other.vec));
    }

    union {
        struct {
            int32_t x = 0;
            int32_t y = 0;
            int32_t z = 0;
        };

        __m128i vec;
    };
};

class d3_vec {
public:
    [[nodiscard]] double len() const {
        return sqrt(x * x + y * y + z * z);
    }

    [[nodiscard]] double distanceSquared(const d3_vec &other) const {
        const double dx = x - other.x;
        const double dy = y - other.y;
        const double dz = z - other.z;

        return dx * dx + dy * dy + dz * dz;
    }

    FORCE_INLINE d3_vec(const double x, const double y, const double z) {
        this->vec = _mm256_setr_pd(x, y, z, 0.0);
    }

    FORCE_INLINE d3_vec(__m256d vec) {
        this->vec = vec;
    }

    FORCE_INLINE d3_vec() {
        this->vec = _mm256_setzero_pd();
    }

    d3_vec operator+(const d3_vec &other) const {
        d3_vec result = *this; // Copy
        result += other;
        return result;
    }

    d3_vec operator+(const double scalar) const {
        d3_vec result = *this; // Copy
        result += scalar;
        return result;
    }

    d3_vec &operator+=(const double scalar) {
        __m256d scalar_vec = _mm256_set1_pd(scalar);
        this->vec = _mm256_add_pd(vec, scalar_vec);
        return *this;
    }

    d3_vec &operator+=(const d3_vec &other) {
        this->vec = _mm256_add_pd(this->vec, other.vec);
        return *this;
    }

    d3_vec operator-(const d3_vec &other) const {
        d3_vec result = *this;
        result -= other;
        return result;
    }

    d3_vec operator-(const double scalar) const {
        d3_vec result = *this;
        result -= scalar;
        return result;
    }

    d3_vec &operator-=(const double scalar) {
        __m256d scalar_vec = _mm256_set1_pd(scalar);
        this->vec = _mm256_sub_pd(this->vec, scalar_vec);
        return *this;
    }

    d3_vec &operator-=(const d3_vec &other) {
        this->vec = _mm256_sub_pd(this->vec, other.vec);
        return *this;
    }

    d3_vec operator*(const d3_vec &other) const {
        d3_vec result = *this;
        result *= other;
        return result;
    }

    d3_vec operator*=(const d3_vec &other) {
        this->vec = _mm256_mul_pd(this->vec, other.vec);
        return *this;
    }

    d3_vec operator/(const d3_vec &other) const {
        d3_vec result = *this;
        result /= other;
        return result;
    }

    d3_vec operator/(const double scalar) const {
        d3_vec result = *this;
        result /= scalar;
        return result;
    }

    d3_vec operator/=(const double scalar) {
        __m256d scalar_vec = _mm256_set1_pd(scalar);
        this->vec = _mm256_div_pd(this->vec, scalar_vec);
        return *this;
    }

    d3_vec operator/=(const d3_vec &other) {
        this->vec = _mm256_div_pd(this->vec, other.vec);
        return *this;
    }

    union {
        struct {
            double x{0.0};
            double y{0.0};
            double z{0.0};
        };

        __m256d vec;
    };
};

class simd_vector_8x3i {
public:
    simd_vector_8x3i() {
        x_vec = _mm256_setzero_si256();
        y_vec = _mm256_setzero_si256();
        z_vec = _mm256_setzero_si256();
    }

    simd_vector_8x3i(int32_t x[8], int32_t y[8], int32_t z[8]) {
        x_vec = _mm256_loadu_si256(reinterpret_cast<__m256i *>(x));
        y_vec = _mm256_loadu_si256(reinterpret_cast<__m256i *>(y));
        z_vec = _mm256_loadu_si256(reinterpret_cast<__m256i *>(z));
    }

    simd_vector_8x3i &operator+=(const simd_vector_8x3i &other) {
        x_vec = _mm256_add_epi32(x_vec, other.x_vec);
        y_vec = _mm256_add_epi32(y_vec, other.y_vec);
        z_vec = _mm256_add_epi32(z_vec, other.z_vec);
        return *this;
    }

    simd_vector_8x3i operator+(const simd_vector_8x3i &other) const {
        simd_vector_8x3i result = *this;
        result += other;
        return result;
    }

    FORCE_INLINE void blendv_inplace(const simd_vector_8x3i &other, const __m256i &mask) {
        x_vec = _mm256_blendv_epi8(x_vec, other.x_vec, mask);
        y_vec = _mm256_blendv_epi8(y_vec, other.y_vec, mask);
        z_vec = _mm256_blendv_epi8(z_vec, other.z_vec, mask);
    }

    union {
        int32_t x[8];
        __m256i x_vec;
    };

    union {
        int32_t y[8];
        __m256i y_vec;
    };

    union {
        int32_t z[8];
        __m256i z_vec;
    };
};

inline void setToVec3iFloored(const d3_vec *src, i3_vec32 *dst) {
    dst->x = floor(src->x);
    dst->y = floor(src->y);
    dst->z = floor(src->z);
}


#endif //NATIVES_VECTOR_H
