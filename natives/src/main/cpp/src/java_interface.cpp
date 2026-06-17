//
// Created by pianoman911 on 05.06.26.
//

#include "occlusion_cache.h"
#include "occlusion_instance.h"

extern "C" {
WorldCache *create_world_cache() {
    return new WorldCache();
}

occlusion_instance *create_occlusion_instance(const dynamic_world *dynamic_world) {
    return new occlusion_instance(dynamic_world);
}

dynamic_world *create_dynamic_world() {
    return new dynamic_world();
}

bool cpp_occlusion_instance_is_aabb_visible(const occlusion_instance *instance, const double min_x, const double min_y, const double min_z,
                         const double max_x, const double max_y, const double max_z, const double viewer_x,
                         const double viewer_y, const double viewer_z) {
    const auto viewer_pos = d3_vec(viewer_x, viewer_y, viewer_z);
    return instance->is_aabb_visible(min_x, min_y, min_z, max_x, max_y, max_z, &viewer_pos);
}

bool cpp_world_cache_has_chunk(WorldCache *instance, const int32_t cx, const int32_t cz) {
    return instance->has_chunk(cx, cz);
}

void cpp_world_cache_remove_chunk(WorldCache *instance, const int32_t cx, const int32_t cz) {
    instance->remove_chunk(cx, cz);
}

void cpp_world_cache_insert_or_update(WorldCache *instance, const int32_t cx, const int32_t cz, const int32_t minY,
                                      const int32_t maxY,
                                      const uint32_t *source_data) {
    instance->insert_or_update(cx, cz, minY, maxY, source_data);
}

void cpp_dynamic_world_resize(dynamic_world *instance, const int32_t radius) {
    instance->resize(radius);
}

void cpp_dynamic_world_update_grid(dynamic_world *instance, const int32_t ccx, const int32_t ccz, WorldCache &world) {
    instance->update_grid(ccx, ccz, world);
}
}
