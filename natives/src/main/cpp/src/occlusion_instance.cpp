//
// Created by pianoman911 on 02.06.26.
//

#include "occlusion_instance.h"

#include <algorithm>
#include <iterator>
#include <immintrin.h>

#include "util.h"

#define SAFE_POINT_OFFSET 0.05
#define TWO_SAFE_POINT_OFFSET (2.0 * SAFE_POINT_OFFSET)
#define POINT_START SAFE_POINT_OFFSET
#define POINT_END (1.0 - SAFE_POINT_OFFSET)
#define POINT_MIDDLE (1.0 / 2.0)
#define DELTA 1.0
#define GRID_SIZE _BV(21) // Support for 1024 blocks

alignas(32) const auto V_POINT_END = d3_vec(POINT_END, POINT_END, POINT_END);
alignas(32) const auto V_SAFE_OFFSET = d3_vec(SAFE_POINT_OFFSET, SAFE_POINT_OFFSET, SAFE_POINT_OFFSET);
alignas(32) const auto V_TWO_OFFSET = d3_vec(TWO_SAFE_POINT_OFFSET, TWO_SAFE_POINT_OFFSET, TWO_SAFE_POINT_OFFSET);
alignas(32) const auto V_POINT_MIDDLE = d3_vec(POINT_MIDDLE, POINT_MIDDLE, POINT_MIDDLE);
alignas(32) const __m256i V_ZERO = _mm256_setzero_si256();
alignas(32) const __m256i V_ONE = {1, 1, 1, 1};
alignas(32) const __m256i V_NEGATIVE_ONE = {-1, -1, -1, -1};
alignas(32) const __m256i V_31 = _mm256_set1_epi32(31);
alignas(32) const __m256i V_GRID_SIZE = {GRID_SIZE, GRID_SIZE, GRID_SIZE, GRID_SIZE};

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
                      double target_x, double target_y, double target_z,
                      uint8_t face_data, uint8_t visible_on_face,
                      double max_x, double max_y, double max_z);

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

inline d3_vec safe_point_end_offset(const d3_vec &target, const d3_vec &max) {
    const d3_vec left_side = target + V_POINT_END;
    const d3_vec right_side = max - V_SAFE_OFFSET;

    return min(left_side, right_side);
}

inline d3_vec safe_middle_offset(const d3_vec &target, const d3_vec max) {
    const d3_vec target_plus_middle = target + V_POINT_MIDDLE;
    const d3_vec max_minus_offset = max - V_SAFE_OFFSET;

    const __m256d mask = _mm256_cmp_pd(target_plus_middle.vec, max_minus_offset.vec, _CMP_LT_OQ);

    const d3_vec diff = max - target - TWO_SAFE_POINT_OFFSET;

    const d3_vec target_plus_offset = target + V_SAFE_OFFSET;
    const auto else_val = d3_vec(_mm256_fmadd_pd(diff.vec, V_POINT_MIDDLE.vec, target_plus_offset.vec));

    return d3_vec(_mm256_blendv_pd(else_val.vec, target_plus_middle.vec, mask));
}

inline void occlusion_instance::prepare_data(const d3_vec *pos_start, const i3_vec32 *voxel_start, const double x,
                                             const double y, const double z) const {
    const auto vec = d3_vec(x, y, z);
    prepare_data(pos_start, voxel_start, &vec);
}

void occlusion_instance::prepare_data(const d3_vec *pos_start, const i3_vec32 *voxel_start,
                                      const d3_vec *data) const {
    d3_vec dir = *data - *pos_start;
    const double dir_len = dir.len();
    dir /= dir_len;
    const double distance_squared = pos_start->distanceSquared(*data);

    double main_pos;
    double second_pos;
    double third_pos;

    double second_direction;
    double third_direction;

    double main_direction = abs(dir.x);

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
    }

    const double distance = main_pos / main_direction;

    buffer_second_error[buffer_pos] = SAFE_FLOOR((second_pos - second_direction * distance) * GRID_SIZE);
    buffer_second_error_step[buffer_pos] = SAFE_FLOOR(second_direction / main_direction * GRID_SIZE);
    buffer_third_error[buffer_pos] = SAFE_FLOOR((third_pos - third_direction * distance) * GRID_SIZE);
    buffer_third_error_step[buffer_pos] = SAFE_FLOOR(third_direction / main_direction * GRID_SIZE);

    if (buffer_second_error[buffer_pos] + buffer_second_error_step[buffer_pos] <= 0) {
        buffer_second_error[buffer_pos] = -buffer_second_error_step[buffer_pos] + 1;
    }

    if (buffer_third_error[buffer_pos] + buffer_third_error_step[buffer_pos] <= 0) {
        buffer_third_error[buffer_pos] = -buffer_third_error_step[buffer_pos] + 1;
    }

    buffer_second_error[buffer_pos] -= GRID_SIZE;
    buffer_third_error[buffer_pos] -= GRID_SIZE;

    const double dirLenSquared = main_direction * main_direction + second_direction * second_direction + third_direction
                                 * third_direction;

    buffer_distance_int[buffer_pos] = SAFE_FLOOR(sqrtf(distance_squared / dirLenSquared) * main_direction);

    buffer_pos_x[buffer_pos] = voxel_start->x;
    buffer_pos_y[buffer_pos] = voxel_start->y;
    buffer_pos_z[buffer_pos] = voxel_start->z;

    auto *mask_ptr = reinterpret_cast<uint32_t *>(&finished_mask); // Mark as not finished
    mask_ptr[buffer_pos] = 0;

    buffer_pos++;
}

bool occlusion_instance::is_aabb_visible(const double min_x, const double min_y, const double min_z,
                                         const double max_x, const double max_y, const double max_z,
                                         const d3_vec *viewer_pos) const {
    setToVec3iFloored(viewer_pos, start_voxel);

    const uint8_t rel_x = relative(min_x, max_x, viewer_pos->x);
    const uint8_t rel_y = relative(min_y, max_y, viewer_pos->y);
    const uint8_t rel_z = relative(min_z, max_z, viewer_pos->z);

    if ((rel_x == REL_INSIDE) && (rel_y == REL_INSIDE) && (rel_z == REL_INSIDE)) {
        return true; // We are inside the aabb, don't cull
    }
    // We are outside the AABB -> Go for culling

    // Loop for voxel positions -> only check the faces that have faces to the outside
    for (double x = min_x; x < max_x - TWO_SAFE_POINT_OFFSET; x++) {
        uint8_t face_edge_data_x = 0; // visible faces on the x-axis
        uint8_t visible_on_face_x = 0; // visible corners on the x-axis

        bool delta_min = delta_lower_than_diff(x, min_x);
        bool delta_max = delta_lower_than_diff(x, max_x);

        // Only check the edges that are outside the AABB
        face_edge_data_x |= delta_min ? ON_MIN_X : 0;
        face_edge_data_x |= delta_max ? ON_MAX_X : 0;

        // Only check the faces that are outside the AABB
        visible_on_face_x |= (delta_min && rel_x == REL_POSITIVE) ? ON_MIN_X : 0;
        visible_on_face_x |= (delta_max && rel_x == REL_NEGATIVE) ? ON_MAX_X : 0;

        // Same for Y and Z
        for (double y = min_y; y < max_y - TWO_SAFE_POINT_OFFSET; y++) {
            // Cascade data
            uint8_t face_edge_data_y = face_edge_data_x;
            uint8_t visible_on_face_y = visible_on_face_x;

            delta_min = delta_lower_than_diff(y, min_y);
            delta_max = delta_lower_than_diff(y, max_y);

            face_edge_data_y |= delta_min ? ON_MIN_Y : 0;
            face_edge_data_y |= delta_max ? ON_MAX_Y : 0;

            visible_on_face_y |= (delta_min && rel_y == REL_POSITIVE) ? ON_MIN_Y : 0;
            visible_on_face_y |= (delta_max && rel_y == REL_NEGATIVE) ? ON_MAX_Y : 0;

            for (double z = min_z; z < max_z - TWO_SAFE_POINT_OFFSET; z++) {
                // Final data holder
                uint8_t face_edge_data = face_edge_data_y;
                uint8_t visible_on_face = visible_on_face_y;

                delta_min = delta_lower_than_diff(z, min_z);
                delta_max = delta_lower_than_diff(z, max_z);

                face_edge_data |= delta_min ? ON_MIN_Z : 0;
                face_edge_data |= delta_max ? ON_MAX_Z : 0;

                visible_on_face |= (delta_min && rel_z == REL_POSITIVE) ? ON_MIN_Z : 0;
                visible_on_face |= (delta_max && rel_z == REL_NEGATIVE) ? ON_MAX_Z : 0;

                if (visible_on_face) {
                    if (this->is_voxel_visible(viewer_pos, start_voxel, x, y, z, face_edge_data, visible_on_face,
                                               max_x, max_y, max_z)) {
                        return true;
                    } else {
                        PRINT("Occluded at %lf, %lf, %lf", x, y, z);
                    }
                }
            }
        }
    }

    PRINT("Not visible");
    return false;
}

bool occlusion_instance::is_voxel_visible(const d3_vec *pos_start, const i3_vec32 *voxel_start,
                                          const double target_x, const double target_y, const double target_z,
                                          const uint8_t face_data, const uint8_t visible_on_face,
                                          const double max_x, const double max_y, const double max_z) const {
    uint16_t dot_selectors = 0; // 8 corners + 6 middle faces -> Cuboid, 14 bools
    const auto target = d3_vec(target_x, target_y, target_z);
    const auto max = d3_vec(max_x, max_y, max_z);

    // Select faces and corners that are visible of the voxel
    if (visible_on_face & ON_MIN_X) {
        dot_selectors |= (1 << 0) | (1 << 8);
        if (face_data & ~ON_MIN_X) {
            dot_selectors |= (1 << 1) | (1 << 4) | (1 << 5);
        }
    }
    if (visible_on_face & ON_MIN_Y) {
        dot_selectors |= (1 << 0) | (1 << 9);
        if (face_data & ~ON_MIN_Y) {
            dot_selectors |= (1 << 3) | (1 << 4) | (1 << 7);
        }
    }
    if (visible_on_face & ON_MIN_Z) {
        dot_selectors |= (1 << 0) | (1 << 10);
        if (face_data & ~ON_MIN_Z) {
            dot_selectors |= (1 << 1) | (1 << 4) | (1 << 5);
        }
    }

    // we assume the target is always at least SAFE_POINT_OFFSET away from max, otherwise there
    // is not enough space to perform a ray cast; this is guaranteed by our for-loop upper bound in #isAABBVisible
    const d3_vec target_begin = d3_vec(target_x, target_y, target_z) + POINT_START;
    const d3_vec target_end = safe_point_end_offset(target, max);

    if (dot_selectors & (1 << 0)) {
        // minX, minY, minZ
        prepare_data(pos_start, voxel_start, &target_begin);
    }

    if (visible_on_face & ON_MAX_Y) {
        dot_selectors |= (1 << 1) | (1 << 12);
        if (face_data & ~ON_MAX_Y) {
            dot_selectors |= (1 << 2) | (1 << 5) | (1 << 6);
        }
    }
    if (dot_selectors & (1 << 1)) {
        // minX, maxY, minZ
        prepare_data(pos_start, voxel_start, target_begin.x, target_end.y, target_begin.z);
    }

    if (visible_on_face & ON_MAX_Z) {
        dot_selectors |= (1 << 2) | (1 << 13);
        if (face_data & ~ON_MAX_Z) {
            dot_selectors |= (1 << 3) | (1 << 6) | (1 << 7);
        }
    }
    if (dot_selectors & (1 << 2)) {
        // minX, maxY, maxZ
        prepare_data(pos_start, voxel_start, target_begin.x, target_end.y, target_end.z);
    }
    if (dot_selectors & (1 << 3)) {
        // minX, minY, maxZ
        prepare_data(pos_start, voxel_start, target_begin.x, target_begin.y, target_end.z);
    }

    if (visible_on_face & ON_MAX_X) {
        dot_selectors |= (1 << 4) | (1 << 11);
        if (face_data & ~ON_MAX_X) {
            dot_selectors |= (1 << 5) | (1 << 6) | (1 << 7);
        }
    }
    if (dot_selectors & (1 << 4)) {
        // maxX, minY, minZ
        prepare_data(pos_start, voxel_start, target_end.x, target_begin.y, target_begin.z);
    }
    if (dot_selectors & (1 << 5)) {
        // maxX, maxY, minZ
        prepare_data(pos_start, voxel_start, target_end.x, target_end.y, target_begin.z);
    }
    if (dot_selectors & (1 << 6)) {
        // maxX, maxY, maxZ
        prepare_data(pos_start, voxel_start, target_end.x, target_end.y, target_end.z);
    }
    if (dot_selectors & (1 << 7)) {
        // maxX, minY, maxZ
        prepare_data(pos_start, voxel_start, target_end.x, target_begin.y, target_end.z);
    }

    if (check_buffer_ready() && simd_raycast()) {
        // Run if buffer is full and first ray cast
        return true;
    }

    // middle points
    d3_vec safe_middle = safe_middle_offset(target, max);

    if (dot_selectors & (1 << 8)) {
        // minX, 0.5, 0.5
        prepare_data(pos_start, voxel_start, target_begin.x, safe_middle.y, safe_middle.z);
        if (check_buffer_ready() && simd_raycast()) {
            // Run if buffer is full and first ray cast
            return true;
        }
    }
    if (dot_selectors & (1 << 9)) {
        // 0.5, minY, 0.5
        prepare_data(pos_start, voxel_start, safe_middle.x, target_begin.y, safe_middle.z);
        if (check_buffer_ready() && simd_raycast()) {
            // Run if buffer is full and first ray cast
            return true;
        }
    }
    if (dot_selectors & (1 << 10)) {
        // 0.5, 0.5, minZ
        prepare_data(pos_start, voxel_start, safe_middle.x, safe_middle.y, target_begin.z);
        if (check_buffer_ready() && simd_raycast()) {
            // Run if buffer is full and first ray cast
            return true;
        }
    }

    if (dot_selectors & (1 << 11)) {
        // maxX, 0.5, 0.5
        prepare_data(pos_start, voxel_start, target_end.x, safe_middle.y, safe_middle.z);
        if (check_buffer_ready() && simd_raycast()) {
            // Run if buffer is full and first ray cast
            return true;
        }
    }
    if (dot_selectors & (1 << 12)) {
        // 0.5, maxY, 0.5
        prepare_data(pos_start, voxel_start, safe_middle.x, target_end.y, safe_middle.z);
        if (check_buffer_ready() && simd_raycast()) {
            // Run if buffer is full and first ray cast
            return true;
        }
    }
    if (dot_selectors & (1 << 13)) {
        // 0.5, 0.5, maxZ
        prepare_data(pos_start, voxel_start, safe_middle.x, safe_middle.y, target_end.z);
        // No run check needed, last one
    }

    // only run if there is actually something to do
    if (buffer_pos) {
        // Run ray cast for buffer_pos's left points
        // Fill up finished mask to prevent calculation of unused buffer place
        for (uint8_t i = buffer_pos; i < SIMD_VECTOR_SIZE; i++) {
            buffer_distance_int[i] = 0; // can't fly that far

            // mark lane as finished
            auto *mask_ptr = reinterpret_cast<uint32_t *>(&finished_mask);
            mask_ptr[i] = 0xFFFFFFFF;
        }
        buffer_pos = 0;

        return simd_raycast();
    }
    return false;
}

inline bool occlusion_instance::check_buffer_ready() const {
    if (buffer_pos == SIMD_VECTOR_SIZE) {
        buffer_pos = 0;
        return true;
    }
    return false;
}

bool occlusion_instance::simd_raycast() const {
    auto pos = simd_vector_8x3i(buffer_pos_x, buffer_pos_y, buffer_pos_z);

    __m256i current_distance = _mm256_setzero_si256();
    const __m256i max_distance = LOAD_PTR_SIMD(buffer_distance_int);

    __m256i second_error = LOAD_PTR_SIMD(buffer_second_error);
    const __m256i second_error_step = LOAD_PTR_SIMD(buffer_second_error_step);

    __m256i third_error = LOAD_PTR_SIMD(buffer_third_error);
    const __m256i third_error_step = LOAD_PTR_SIMD(buffer_third_error_step);

    const auto main_step = simd_vector_8x3i(buffer_main_step_x, buffer_main_step_y, buffer_main_step_z);
    auto main_second_step = simd_vector_8x3i(buffer_second_step_x, buffer_second_step_y, buffer_second_step_z);
    main_second_step += main_step;

    auto main_third_step = simd_vector_8x3i(buffer_third_step_x, buffer_third_step_y, buffer_third_step_z);
    const simd_vector_8x3i main_second_third_step = main_second_step + main_third_step;
    main_third_step += main_step; // add after line above

    while (true) {
        // TODO: failsafe
        __m256i dist_reached = _mm256_cmpgt_epi32(current_distance, max_distance);

        if (__m256i just_reached = _mm256_andnot_si256(finished_mask, dist_reached);
            _mm256_movemask_epi8(just_reached) != 0) {
            PRINT("Distance reached for some rays, stopping ray cast!");
            return true;
        }

        if (_mm256_movemask_epi8(finished_mask) == 0xFFFFFFFF) {
            break;
        }

        current_distance = _mm256_add_epi32(current_distance, V_ONE);
        second_error = _mm256_add_epi32(second_error, second_error_step);
        third_error = _mm256_add_epi32(third_error, third_error_step);

        __m256i sec_gt_0 = _mm256_cmpgt_epi32(second_error, V_ZERO);
        __m256i thi_gt_0 = _mm256_cmpgt_epi32(third_error, V_ZERO);

        __m256i cond_both = _mm256_and_si256(sec_gt_0, thi_gt_0);
        __m256i cond_second = _mm256_andnot_si256(cond_both, second_error);
        __m256i cond_third = _mm256_andnot_si256(cond_both, third_error);

        simd_vector_8x3i step = main_step;

        step.blendv_inplace(main_third_step, cond_third);
        step.blendv_inplace(main_second_step, cond_second);
        step.blendv_inplace(main_second_third_step, cond_both);

        pos += step;

        __m256i sub_sec = _mm256_and_si256(_mm256_or_si256(cond_both, cond_second), V_GRID_SIZE);
        __m256i sub_thi = _mm256_and_si256(_mm256_or_si256(cond_both, cond_third), V_GRID_SIZE);
        second_error = _mm256_sub_epi32(second_error, sub_sec);
        third_error = _mm256_sub_epi32(third_error, sub_thi);

        // Pos2Chunk_Shift (4+1); /16 block -> chunk; /2 (2x2x2)-voxels -> real coords
        __m256i chunk_x = _mm256_srai_epi32(pos.x_vec, 5);
        __m256i chunk_z = _mm256_srai_epi32(pos.z_vec, 5);

        // Local coords (0 - 31)
        __m256i local_x = _mm256_and_si256(pos.x_vec, V_31);
        __m256i local_z = _mm256_and_si256(pos.z_vec, V_31);

        __m256i grid_x = _mm256_sub_epi32(chunk_x, cached_world_data.ocx);
        __m256i grid_z = _mm256_sub_epi32(chunk_z, cached_world_data.ocz);

        // Radius-Check
        __m256i gx_valid = _mm256_and_si256(_mm256_cmpgt_epi32(grid_x, V_NEGATIVE_ONE),
                                            _mm256_cmpgt_epi32(cached_world_data.side_length, grid_x));
        __m256i gz_valid = _mm256_and_si256(_mm256_cmpgt_epi32(grid_z, V_NEGATIVE_ONE),
                                            _mm256_cmpgt_epi32(cached_world_data.side_length, grid_z));
        __m256i grid_valid_mask = _mm256_cmpgt_epi32(gx_valid, gz_valid);

        // Calculate area
        __m256i grid_indicis = _mm256_add_epi32(_mm256_mullo_epi32(grid_z, cached_world_data.side_length), grid_x);
        grid_indicis = _mm256_and_si256(grid_indicis, grid_valid_mask);

        // Collect data scalar
        alignas(32) uint32_t extracted_bits[SIMD_VECTOR_SIZE] = {};

        alignas(32) int32_t rx[SIMD_VECTOR_SIZE], ry[SIMD_VECTOR_SIZE], rz[SIMD_VECTOR_SIZE], r_gvalid[SIMD_VECTOR_SIZE]
                , r_gidx[SIMD_VECTOR_SIZE];
        _mm256_storeu_si256(reinterpret_cast<__m256i *>(rx), local_x);
        _mm256_storeu_si256(reinterpret_cast<__m256i *>(ry), pos.y_vec);
        _mm256_storeu_si256(reinterpret_cast<__m256i *>(rz), local_z);
        _mm256_storeu_si256(reinterpret_cast<__m256i *>(r_gvalid), grid_valid_mask);
        _mm256_storeu_si256(reinterpret_cast<__m256i *>(r_gidx), grid_indicis);

        int32_t fin_mask = _mm256_movemask_ps(_mm256_castsi256_ps(finished_mask));

        for (uint8_t i = 0; i < SIMD_VECTOR_SIZE; i++) {
            if (!((fin_mask >> i) & 1)) {
                // If not finished
                if (r_gvalid[i]) {
                    if (occlusion_chunk *ch = world->chunks[r_gidx[i]]; ch != nullptr) {
                        if (ry[i] >= ch->minY && ry[i] <= ch->maxY) {
                            int32_t local_y = ry[i] - ch->minY;
                            int32_t array_index = (local_y << 5) + rz[i];

                            uint32_t bit_row = ch->layers[array_index];
                            extracted_bits[i] = (bit_row >> rx[i]) & 1;
                        }
                    }
                }
            }
        }

        __m256i opaque_simd = _mm256_setzero_si256();
        auto *opaque_ptr = reinterpret_cast<int32_t *>(&opaque_simd);
        for (int i = 0; i < 8; ++i) {
            if (extracted_bits[i]) opaque_ptr[i] = 0xFFFFFFFF;
        }

        if (int32_t opaque_mask = _mm256_movemask_ps(_mm256_castsi256_ps(opaque_simd)); opaque_mask != 0) {
            auto *mask_ptr = reinterpret_cast<int32_t *>(&finished_mask);
            for (uint8_t i = 0; i < SIMD_VECTOR_SIZE; i++) {
                if ((opaque_mask & (1 << i)) && !((fin_mask >> i) & 1)) {
                    mask_ptr[i] = 0xFFFFFFFF;
                }
            }
        }
    }

    return false;
}
