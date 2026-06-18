//
// Created by pianoman911 on 04.06.26.
//

#ifndef NATIVES_OCCLUSION_CHUNK_H
#define NATIVES_OCCLUSION_CHUNK_H
#include <cstdint>
#include <shared_mutex>
#include <unordered_map>
#include <cstdlib>
#include <cstring>
#include <mutex>
#include <ranges>

#include "util.h"

constexpr int32_t Y_LAYER_SIZE_BYTES = 128; // 128 Bytes, 32 * sizeof(uint32_t)

struct occlusion_chunk {
    int32_t minY;
    int32_t maxY;

    uint32_t *layers;
};

class WorldCache {
private:
    std::shared_mutex mutex;
    std::unordered_map<uint32_t, occlusion_chunk *> cache;

    static uint32_t get_key(const int32_t cx, const int32_t cz) {
        return (static_cast<uint32_t>(cx) << 16) | (static_cast<uint32_t>(cz));
    }

public:
    ~WorldCache() {
        for (const auto &chunk: cache | std::views::values) {
            if (chunk) {
                std::free(chunk->layers);
                delete chunk;
            }
        }
    }

    void insert_or_update(const int32_t cx, const int32_t cz, const int32_t minY, const int32_t maxY,
                          const uint32_t *source_data) {
        std::unique_lock lock(mutex);

        const uint64_t key = get_key(cx, cz);

        const int32_t num_layers = (maxY - minY + 1);
        const size_t total_bytes = num_layers * Y_LAYER_SIZE_BYTES;

        const auto it = cache.find(key);
        occlusion_chunk *chunk;
        if (it == cache.end()) {
            chunk = new occlusion_chunk();
            chunk->layers = nullptr;
            cache[key] = chunk;
        } else {
            chunk = it->second;
            std::free(chunk->layers);
        }

        chunk->minY = minY;
        chunk->maxY = maxY;

        chunk->layers = static_cast<uint32_t *>(std::aligned_alloc(32, total_bytes));

        if (source_data != nullptr) {
            std::memcpy(chunk->layers, source_data, total_bytes);
            PRINT("Inserted/Updated chunk at (%d, %d) with minY: %d, maxY: %d", cx, cz, minY, maxY);
        }
    }

    occlusion_chunk *get_chunk(const int32_t cx, const int32_t cz) {
        std::shared_lock lock(mutex);
        const uint64_t key = get_key(cx, cz);
        if (const auto it = cache.find(key); it != cache.end()) {
            return it->second;
        }
        return nullptr;
    }

    void remove_chunk(const int32_t cx, const int32_t cz) {
        std::unique_lock lock(mutex);

        const uint64_t key = get_key(cx, cz);
        if (const auto it = cache.find(key); it != cache.end()) {
            std::free(it->second->layers);

            delete it->second;
            cache.erase(it);
        }
    }

    bool has_chunk(const int32_t cx, const int32_t cz) {
        std::shared_lock lock(mutex);
        const uint64_t key = get_key(cx, cz);
        return cache.find(key) != cache.end();
    }
};

struct dynamic_world {
    int32_t chunk_radius;
    int32_t side_length; // chunk_radius * 2 + 1

    occlusion_chunk **chunks; // Flat array

    int32_t ocx;
    int32_t ocz;

    void resize(const int32_t radius) {
        chunk_radius = radius;
        side_length = chunk_radius * 2 + 1;
        chunks = new occlusion_chunk *[side_length * side_length];

        // fill chunks with null pointers
        memset(chunks, 0, sizeof(occlusion_chunk *) * side_length * side_length);
    }

    void update_grid(const int32_t ccx, const int32_t ccz, WorldCache *world) {
        // upper left corner of the grid
        ocx = ccx - chunk_radius;
        ocz = ccz - chunk_radius;

        for (int32_t lz = 0; lz < side_length; lz++) {
            for (int32_t lx = 0; lx < side_length; lx++) {
                // Absolute chunk coords
                const int32_t acx = ocx + lx;
                const int32_t acz = ocz + lz;

                const int32_t grid_index = (lz * side_length) + lx;

                chunks[grid_index] = world->get_chunk(acx, acz);
            }
        }
    }

    void free_grid() const {
        delete[] chunks;
    }
};

#endif //NATIVES_OCCLUSION_CHUNK_H
