//
// Created by pianoman911 on 02.06.26.
//

#ifndef NATIVES_UTIL_H
#define NATIVES_UTIL_H

#include <cmath>
#include <chrono>
#include <thread>

#include "vector.h"

#define _BV(bit) (1 << (bit))

#define SAFE_FLOOR(x) static_cast<int32_t>(std::floor(x))
#define LOAD_PTR_SIMD(ptr) _mm256_loadu_si256(reinterpret_cast<const __m256i *>(ptr))


#define PRINT(msg, ...) printf(msg, ##__VA_ARGS__); \
printf("\n"); \
fflush(stdout);

inline d3_vec min(const d3_vec &a, const d3_vec &b) {
    const __m256d result = _mm256_min_pd(a.vec, b.vec);
    return d3_vec(result);
}

#endif //NATIVES_UTIL_H
