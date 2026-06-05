//
// Created by pianoman911 on 05.06.26.
//

#include "occlusion_cache.h"
#include "occlusion_instance.h"

extern "C" {
ChunkCache *create_chunk_cache() {
    return new ChunkCache();
}

occlusion_instance *create_occlusion_instance() {
    return new occlusion_instance();
}

bool cpp_is_aabb_visible(const occlusion_instance *instance, const double min_x, const double min_y, const double min_z,
                         const double max_x, const double max_y, const double max_z, const double viewer_x,
                         const double viewer_y, const double viewer_z) {
    const auto viewer_pos = d3_vec(viewer_x, viewer_y, viewer_z);
    return instance->is_aabb_visible(min_x, min_y, min_z, max_x, max_y, max_z, &viewer_pos);
}
}
