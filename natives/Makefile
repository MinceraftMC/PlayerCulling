ifeq ($(OS),Windows_NT)
    TARGET = build/libs/natives/windows_x64/playerculling_avx2.dll
    CC = g++
    CFLAGS = -O3 -shared -mavx2 -mfma
else
    TARGET = build/libs/natives/linux_x64/playerculling_avx2.so
    CC = g++
    CFLAGS = -O3 -shared -fPIC -mavx2 -mfma
endif

SRC_DIR = src/main/cpp/src

SRC_FILES = simd_culling.cpp \
			occlusion_instance.cpp \
			java_interface.cpp

SRC = $(addprefix $(SRC_DIR)/, $(SRC_FILES))

CFLAGS += -I$(SRC_DIR)/include

all: $(TARGET)

$(TARGET): $(SRC)
	@mkdir -p $(dir $(TARGET)) 2>/dev/null || mkdirs $(dir $(TARGET)) 2>/dev/null || true
	$(CC) $(CFLAGS) $(SRC) -o $(TARGET)

clean:
	rm -rf build/libs/natives/*