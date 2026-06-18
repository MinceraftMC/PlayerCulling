//
// Created by pianoman911 on 02.06.26.
//

#ifndef NATIVES_OCCLUSION_INSTANCE_H
#define NATIVES_OCCLUSION_INSTANCE_H

#include "vector.h"
#include "occlusion_cache.h"
#ifndef __STDC_IEC_559__
#error "Requires IEEE 754 floating point!"
#endif

#define SIMD_VECTOR_SIZE 8
#define CREATE_SIMD_QUEUE_BUFFER(name) \
   mutable int32_t buffer_##name[SIMD_VECTOR_SIZE] = {};

#define CREATE_3AXIS_SIMD_QUEUE_BUFFER(name) \
    CREATE_SIMD_QUEUE_BUFFER(name##_x) \
    CREATE_SIMD_QUEUE_BUFFER(name##_y) \
    CREATE_SIMD_QUEUE_BUFFER(name##_z)

class occlusion_instance {
    struct cached_world_data {
        __m256i ocx = _mm256_setzero_si256();
        __m256i ocz = _mm256_setzero_si256();

        __m256i side_length = _mm256_setzero_si256();

        void update_world(const dynamic_world &dynamic_world) {
            ocx = _mm256_set1_epi32(dynamic_world.ocx);
            ocz = _mm256_set1_epi32(dynamic_world.ocz);
            side_length = _mm256_set1_epi32(dynamic_world.side_length);
        }
    };

private:
    mutable uint8_t buffer_pos = 0;

    CREATE_3AXIS_SIMD_QUEUE_BUFFER(pos)
    CREATE_SIMD_QUEUE_BUFFER(second_error)
    CREATE_SIMD_QUEUE_BUFFER(third_error)
    CREATE_SIMD_QUEUE_BUFFER(second_error_step)
    CREATE_SIMD_QUEUE_BUFFER(third_error_step)
    CREATE_3AXIS_SIMD_QUEUE_BUFFER(main_step)
    CREATE_3AXIS_SIMD_QUEUE_BUFFER(second_step)
    CREATE_3AXIS_SIMD_QUEUE_BUFFER(third_step)
    CREATE_SIMD_QUEUE_BUFFER(distance_int)

    i3_vec32 *start_voxel = new i3_vec32();

    cached_world_data cached_world_data;

    dynamic_world *world;

    mutable __m256i finished_mask = _mm256_setzero_si256();

    inline bool check_buffer_ready() const;

    bool simd_raycast() const;

    inline void prepare_data(const d3_vec *pos_start, const i3_vec32 *voxel_start, double x, double y, double z) const;

    void prepare_data(const d3_vec *pos_start, const i3_vec32 *voxel_start, const d3_vec *data) const;

    bool is_voxel_visible(const d3_vec *pos_start, const i3_vec32 *voxel_start,
                          double target_x, double target_y, double target_z,
                          uint8_t face_data, uint8_t visible_on_face,
                          double max_x, double max_y, double max_z) const;

public:
    explicit occlusion_instance(dynamic_world *world) {
        this->world = world;
    }

    bool is_aabb_visible(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z,
                         const d3_vec *viewer_pos) const;

    void update_world(int32_t ccx, int32_t ccz, WorldCache *_world);
};


#endif //NATIVES_OCCLUSION_INSTANCE_H
