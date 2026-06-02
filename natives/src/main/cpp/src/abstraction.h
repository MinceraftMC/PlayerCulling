//
// Created by pianoman911 on 03.06.26.
//

#ifndef NATIVES_ABSTRACTION_H
#define NATIVES_ABSTRACTION_H

#include "util.h"

#define BLOCK_FACE_UP _BV(0)
#define BLOCK_FACE_DOWN _BV(1)
#define BLOCK_FACE_NORTH _BV(2)
#define BLOCK_FACE_SOUTH _BV(3)
#define BLOCK_FACE_WEST _BV(4)
#define BLOCK_FACE_EAST _BV(5)

#define BLOCK_FACE_UP_MOD i3_vec32(0, 1, 0)
#define BLOCK_FACE_DOWN_MOD i3_vec32(0, -1, 0)

#endif //NATIVES_ABSTRACTION_H
