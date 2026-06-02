//
// Created by pianoman911 on 02.06.26.
//

#include "occlusion_instance.h"

#include <algorithm>
#include <avx2intrin.h>
#include <avx512vlintrin.h>
#include <avxintrin.h>
#include <iterator>
#include <immintrin.h>

#include "util.h"
#include "abstraction.h"

#define SAFE_POINT_OFFSET 0.05
#define TWO_SAFE_POINT_OFFSET (2.0 * SAFE_POINT_OFFSET)
#define POINT_START SAFE_POINT_OFFSET
#define POINT_END (1.0 - SAFE_POINT_OFFSET)
#define POINT_MIDDLE (1.0 / 2.0)
#define DELTA 1.0
#define GRID_SIZE _BV(21) // Support for 1024 blocks

#define ON_MIN_X _BV(0)
#define ON_MAX_X _BV(1)
#define ON_MIN_Y _BV(2)
#define ON_MAX_Y _BV(3)
#define ON_MIN_Z _BV(4)
#define ON_MAX_Z _BV(5)

#define REL_INSIDE _BV(0)
#define REL_POSITIVE _BV(1)
#define REL_NEGATIVE _BV(2)

bool is_voxel_visible(const d3_vec *pos_start, const i3_vec32 *voxel_start,
                      const double target_x, const double target_y, const double target_z,
                      const uint8_t face_data, const uint8_t visible_on_face,
                      const double max_x, const double max_y, const double max_z);

bool simd_raycast();

inline uint8_t relative(const double min, const double max, const double pos) {
    if (max > pos && min > pos) {
        return REL_POSITIVE;
    }
    if (max < pos && min < pos) {
        return REL_NEGATIVE;
    }
    return REL_INSIDE;
}

inline bool delta_lower_than_diff(const double a, const double b) {
    return fabs(a - b) < DELTA;
}

inline double get_pos(const double dir, const double pos, const double step) {
    return dir > 0 ? (pos - step) : (step + 1 - pos);
}

void occlusion_instance::prepare_data(const d3_vec *pos_start, const i3_vec32 *voxel_start,
                                      const d3_vec *data) const {
    d3_vec dir = *data - *pos_start;
    double dir_len = dir.len();
    dir /= dir_len;
    double distance_squared = pos_start->distanceSquared(*data);

    double main_pos;
    double second_pos;
    double third_pos;

    double main_direction;
    double second_direction;
    double third_direction;

    main_direction = abs(dir.x);

    if (abs(dir.y) > main_direction) {
        main_direction = abs(dir.y);
        if (abs(dir.z) > main_direction) {
            main_direction = abs(dir.z);

            buffer_main_step_z[buffer_pos] = dir.z > 0 ? 1 : -1;
            main_pos = get_pos(dir.z, pos_start->z, voxel_start->z);

            second_direction = abs(dir.x);
            buffer_second_step_x[buffer_pos] = dir.x > 0 ? 1 : -1;
            second_pos = get_pos(dir.x, pos_start->x, voxel_start->x);

            third_direction = abs(dir.y);
            buffer_third_step_y[buffer_pos] = dir.y > 0 ? 1 : -1;
            third_pos = get_pos(dir.y, pos_start->y, voxel_start->y);
        } else {
            buffer_main_step_y[buffer_pos] = dir.y > 0 ? 1 : -1;
            main_pos = get_pos(dir.y, pos_start->y, voxel_start->y);

            second_direction = abs(dir.z);
            buffer_second_step_z[buffer_pos] = dir.z > 0 ? 1 : -1;
            second_pos = get_pos(dir.z, pos_start->z, voxel_start->z);

            third_direction = abs(dir.x);
            buffer_third_step_x[buffer_pos] = dir.x > 0 ? 1 : -1;
            third_pos = get_pos(dir.x, pos_start->x, voxel_start->x);
        }
    } else {
        if (abs(dir.z) > main_direction) {
            main_direction = abs(dir.z);

            buffer_main_step_z[buffer_pos] = dir.z > 0 ? 1 : -1;
            main_pos = get_pos(dir.z, pos_start->z, voxel_start->z);

            second_direction = abs(dir.x);
            buffer_second_step_x[buffer_pos] = dir.x > 0 ? 1 : -1;
            second_pos = get_pos(dir.x, pos_start->x, voxel_start->x);

            third_direction = abs(dir.y);
            buffer_third_step_y[buffer_pos] = dir.y > 0 ? 1 : -1;
            third_pos = get_pos(dir.y, pos_start->y, voxel_start->y);
        } else {
            buffer_main_step_x[buffer_pos] = dir.x > 0 ? 1 : -1;
            main_pos = get_pos(dir.x, pos_start->x, voxel_start->x);

            second_direction = abs(dir.y);
            buffer_second_step_y[buffer_pos] = dir.y > 0 ? 1 : -1;
            second_pos = get_pos(dir.y, pos_start->y, voxel_start->y);

            third_direction = abs(dir.z);
            buffer_third_step_z[buffer_pos] = dir.z > 0 ? 1 : -1;
            third_pos = get_pos(dir.z, pos_start->z, voxel_start->z);
        }

        const double distance = main_pos / main_direction;

        buffer_second_error[buffer_pos] = SAFE_FLOOR((second_pos - second_direction * distance) * GRID_SIZE);
        buffer_second_error_step[buffer_pos] = SAFE_FLOOR(second_direction / main_direction * GRID_SIZE);
        buffer_third_error[buffer_pos] = SAFE_FLOOR((third_pos - third_direction * distance) * GRID_SIZE);
        buffer_third_error_step[buffer_pos] = SAFE_FLOOR(third_direction / main_direction * GRID_SIZE);

        if (buffer_second_error[buffer_pos] + buffer_second_error_step[buffer_pos] <= 0) {
            buffer_second_error[buffer_pos] = -buffer_second_error[buffer_pos] + 1;
        }

        if (buffer_third_error[buffer_pos] + buffer_third_error_step[buffer_pos] <= 0) {
            buffer_third_error[buffer_pos] = -buffer_third_error[buffer_pos] + 1;
        }

        buffer_second_error[buffer_pos] -= GRID_SIZE;
        buffer_third_error[buffer_pos] -= GRID_SIZE;

        double dirLenSquared = main_direction * main_direction + second_direction * second_direction + third_direction *
                               third_direction;

        buffer_distance_int[buffer_pos] = SAFE_FLOOR((distance_squared / dirLenSquared) * main_direction);
    }
}

bool occlusion_instance::is_aabb_visible(const double min_x, const double min_y, const double min_z, const double max_x,
                                         const double max_y, const double max_z,
                                         const d3_vec *viewer_pos) const {
    setToVec3iFloored(viewer_pos, start_voxel);

    uint8_t rel_x = relative(min_x, max_x, viewer_pos->x);
    uint8_t rel_y = relative(min_y, max_y, viewer_pos->y);
    uint8_t rel_z = relative(min_z, max_z, viewer_pos->z);

    if ((rel_x & rel_y & rel_z) == 0) {
        return true; // We are inside the aabb, don't cull
    }

    // Loop for voxel positions -> only check the faces that have faces to the outside
    for (double x = min_x; x < max_x - TWO_SAFE_POINT_OFFSET; x++) {
        uint8_t face_edge_data_x = 0;
        uint8_t visible_on_face_x = 0;

        bool delta_min = delta_lower_than_diff(x, min_x);
        bool delta_max = delta_lower_than_diff(x, max_x);

        // Only check the edges that are outside the AABB
        face_edge_data_x |= delta_min ? ON_MIN_X : 0;
        face_edge_data_x |= delta_max ? ON_MAX_X : 0;

        // Only check the faces that are outside the AABB
        visible_on_face_x |= (delta_min && rel_x & REL_POSITIVE) ? ON_MIN_X : 0;
        visible_on_face_x |= (delta_max && rel_x & REL_NEGATIVE) ? ON_MAX_X : 0;

        // Same for Y and Z

        for (double y = min_y; y < max_y - TWO_SAFE_POINT_OFFSET; y++) {
            // Cascade data
            uint8_t face_edge_data_y = face_edge_data_x;
            uint8_t visible_on_face_y = face_edge_data_y;

            delta_min = delta_lower_than_diff(y, min_y);
            delta_max = delta_lower_than_diff(y, max_y);

            face_edge_data_y |= delta_min ? ON_MIN_Y : 0;
            face_edge_data_y |= delta_max ? ON_MAX_Y : 0;

            visible_on_face_y |= (delta_min && rel_y & REL_POSITIVE) ? ON_MIN_Y : 0;
            visible_on_face_y |= (delta_max && rel_y & REL_NEGATIVE) ? ON_MAX_Y : 0;

            for (double z = min_z; z < max_z - TWO_SAFE_POINT_OFFSET; z++) {
                // Final data holder
                uint8_t face_edge_data = face_edge_data_y;
                uint8_t visible_on_face = face_edge_data;

                delta_min = delta_lower_than_diff(z, min_z);
                delta_max = delta_lower_than_diff(z, max_z);

                face_edge_data |= delta_min ? ON_MIN_Z : 0;
                face_edge_data |= delta_max ? ON_MAX_Z : 0;

                if (visible_on_face) {
                    if (this->is_voxel_visible(viewer_pos, start_voxel, x, y, z, face_edge_data, visible_on_face,
                                               max_x, max_y, max_z)) {
                        return true;
                    }
                }
            }
        }
    }

    return false;
}

bool occlusion_instance::is_voxel_visible(const d3_vec *pos_start, const i3_vec32 *voxel_start,
                                          const double target_x, const double target_y, const double target_z,
                                          const uint8_t face_data, const uint8_t visible_on_face,
                                          const double max_x, const double max_y, const double max_z) const {
    uint16_t dot_selectors = 0; // 8 corners + 6 middle faces -> Cuboid, 14 bools

    // Select faces and corners that are visible of the voxel
    if ((visible_on_face & ON_MIN_X) == ON_MIN_X) {
        dot_selectors |= (1 << 0) | (1 << 8);
        if ((face_data & ~ON_MIN_X) != 0) {
            dot_selectors |= (1 << 1) | (1 << 4) | (1 << 5);
        }
    }
    if ((visible_on_face & ON_MIN_Y) == ON_MIN_Y) {
        dot_selectors |= (1 << 0) | (1 << 9);
        if ((face_data & ~ON_MIN_Y) != 0) {
            dot_selectors |= (1 << 3) | (1 << 4) | (1 << 7);
        }
    }
    if ((visible_on_face & ON_MIN_Z) == ON_MIN_Z) {
        dot_selectors |= (1 << 0) | (1 << 10);
        if ((face_data & ~ON_MIN_Z) != 0) {
            dot_selectors |= (1 << 1) | (1 << 4) | (1 << 5);
        }
    }

    // we assume the target is always at least SAFE_POINT_OFFSET away from max, otherwise there
    // is not enough space to perform a raycast; this is guaranteed by our for-loop upper bound in #isAABBVisible
    d3_vec target_begin = {target_x, target_y, target_z};
    target_begin += POINT_START;

    if (dot_selectors & (1 << 0)) {
        prepare_data(pos_start, voxel_start, &target_begin);
    }
}

bool occlusion_instance::simd_raycast() {
    simd_vector_8x3i pos = simd_vector_8x3i(buffer_pos_x, buffer_pos_y, buffer_pos_z);

    __m256i current_distance = _mm256_setzero_si256();
    __m256i max_distance = _mm256_load_epi32(buffer_distance_int);

    __m256i second_error = _mm256_load_epi32(buffer_second_error);
    __m256i second_error_step = _mm256_load_epi32(buffer_second_error_step);

    __m256i third_error = _mm256_load_epi32(buffer_third_error);
    __m256i third_error_step = _mm256_load_epi32(buffer_third_error_step);

    simd_vector_8x3i main_step = simd_vector_8x3i(buffer_main_step_x, buffer_main_step_y, buffer_main_step_z);
    simd_vector_8x3i main_second_step = simd_vector_8x3i(buffer_second_step_x, buffer_second_step_y,
                                                         buffer_second_step_z);
    main_second_step += main_step;

    simd_vector_8x3i main_third_step = simd_vector_8x3i(buffer_third_step_x, buffer_third_step_y, buffer_third_step_z);
    // Add main afterward, see below
    simd_vector_8x3i main_second_third = main_second_step + main_third_step;
    main_third_step += main_second_step;

    __m256i finished_mask = _mm256_setzero_si256();
    __m256i final_results = _mm256_setzero_si256();

    while (true) {
        // TODO: failsafe
        __m256i dist_reached = _mm256_cmpgt_epi32(current_distance, max_distance);
        __m256i just_reached = _mm256_andnot_si256(finished_mask, dist_reached);

        if (_mm256_movemask_epi8(just_reached) != 0) {
        }
    }
}
