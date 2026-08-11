/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

#include <android/log.h>
#include <cstring>
#include <string>
#include <vector>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <elf.h>
#include <atomic>
#include <jni.h>
#include "helpers.h"

#include "fluoride_hooks.h"

static std::atomic<bool> enableSdpHook(false);

static HookFunType hook_func = nullptr;

static uint8_t (*original_l2c_fcr_chk_chan_modes)(void *) = nullptr;

uint8_t fake_l2c_fcr_chk_chan_modes(void *p_ccb) {
    LOGI("fake_l2c_fcr_chk_chan_modes called");
    uint8_t orig = 0;
    if (original_l2c_fcr_chk_chan_modes)
        orig = original_l2c_fcr_chk_chan_modes(p_ccb);

    LOGI("fake_l2c_fcr_chk_chan_modes: orig = %d, returning 1", orig);
    return 1;
}

static tBTA_STATUS (*original_BTA_DmSetLocalDiRecord)(tSDP_DI_RECORD *, uint32_t *) = nullptr;

tBTA_STATUS fake_BTA_DmSetLocalDiRecord(tSDP_DI_RECORD *p_device_info, uint32_t *p_handle) {

    LOGI("fake_BTA_DmSetLocalDiRecord called");

    if (original_BTA_DmSetLocalDiRecord &&
        enableSdpHook.load(std::memory_order_relaxed))
        original_BTA_DmSetLocalDiRecord(p_device_info, p_handle);

    LOGI("fake_BTA_DmSetLocalDiRecord: modifying vendor to 0x004C, vendor_id_source to 0x0001");

    if (p_device_info) {
        p_device_info->vendor = 0x004C;
        p_device_info->vendor_id_source = 0x0001;
    }

    LOGI("fake_BTA_DmSetLocalDiRecord: returning status %d",
         original_BTA_DmSetLocalDiRecord ? original_BTA_DmSetLocalDiRecord(p_device_info, p_handle)
                                         : BTA_FAILURE);
    return original_BTA_DmSetLocalDiRecord ? original_BTA_DmSetLocalDiRecord(p_device_info,
                                                                             p_handle)
                                           : BTA_FAILURE;
}


static bool hookLibrary(const char *libname) {
    LOGI("hookLibrary called with libname: %s", libname);

    if (!hook_func) {
        LOGE("hook_func not initialized");
        return false;
    }

    std::string path;
    if (!getLibraryPath(libname, path)) {
        LOGE("Failed to locate %s", libname);
        return false;
    }
    LOGI("hookLibrary: located path: %s", path.c_str());

    int fd = open(path.c_str(), O_RDONLY);
    if (fd < 0) {
        LOGE("hookLibrary: open failed");
        return false;
    }

    struct stat st{};
    if (fstat(fd, &st) != 0) {
        LOGE("hookLibrary: fstat failed");
        close(fd);
        return false;
    }
    LOGI("hookLibrary: opened file, size: %lld", (long long) st.st_size);

    std::vector<uint8_t> file(st.st_size);
    read(fd, file.data(), st.st_size);
    close(fd);

    auto *eh = reinterpret_cast<Elf64_Ehdr *>(file.data());
    auto *shdr = reinterpret_cast<Elf64_Shdr *>(
            file.data() + eh->e_shoff);

    const char *shstr = reinterpret_cast<const char *>(
            file.data() + shdr[eh->e_shstrndx].sh_offset);

    uint64_t chk_offset = 0;
    uint64_t sdp_offset = 0;

    for (int i = 0; i < eh->e_shnum; ++i) {
        if (!strcmp(shstr + shdr[i].sh_name, ".gnu_debugdata")) {
            LOGI("hookLibrary: found .gnu_debugdata section");

            std::vector<uint8_t> compressed(file.begin() + shdr[i].sh_offset,
                                            file.begin() + shdr[i].sh_offset + shdr[i].sh_size);

            std::vector<uint8_t> decompressed;

            if (decompressXZ(compressed.data(), compressed.size(), decompressed)) {
                chk_offset = findSymbolOffset(decompressed, "l2c_fcr_chk_chan_modes");
                sdp_offset = findSymbolOffset(decompressed, "BTA_DmSetLocalDiRecord");
            } else {
                LOGE("debugdata decompress failed");
            }

            break;
        }
    }

    if (!chk_offset) {
        LOGI("fallback dynsym chk");
        chk_offset = findSymbolOffsetDynsym(file, "l2c_fcr_chk_chan_modes");
    }

    if (!sdp_offset) {
        LOGI("fallback dynsym sdp");
        sdp_offset = findSymbolOffsetDynsym(file, "BTA_DmSetLocalDiRecord");
    }

    uintptr_t base = getModuleBase(libname);

    if (!base) {
        LOGE("hookLibrary: getModuleBase failed");
        return false;
    }

    if (chk_offset) {
        void *target = reinterpret_cast<void *>(base + chk_offset);
        hook_func(target, (void *) fake_l2c_fcr_chk_chan_modes,
                  (void **) &original_l2c_fcr_chk_chan_modes);
        LOGI("hooked chk");
    }

    if (sdp_offset) {
        void *target = reinterpret_cast<void *>(base + sdp_offset);
        hook_func(target, (void *) fake_BTA_DmSetLocalDiRecord,
                  (void **) &original_BTA_DmSetLocalDiRecord);
        LOGI("hooked sdp");
    }

    return chk_offset || sdp_offset;
}

static void on_library_loaded(const char *name, void *) {
    LOGI("on_library_loaded called with name: %s", name);

    if (strstr(name, "libbluetooth_jni.so")) {
        LOGI("Bluetooth JNI loaded");
        hookLibrary("libbluetooth_jni.so");
    }

    if (strstr(name, "libbluetooth_qti.so")) {
        LOGI("Bluetooth QTI loaded");
        hookLibrary("libbluetooth_qti.so");
    }
}

extern "C" [[gnu::visibility("default")]]
[[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    LOGI("native_init called with entries: %p", entries);
    hook_func = (HookFunType) entries->hook_func;
    LOGI("LibrePodsNativeHook initialized, sdp hook enabled: %d",
         enableSdpHook.load(std::memory_order_relaxed));
    return on_library_loaded;
}

extern "C" JNIEXPORT void JNICALL
Java_me_kavishdevar_librepods_utils_NativeBridge_setSdpHook(JNIEnv *, jobject thiz,
                                                            jboolean enable) {
    LOGI("setSdpHook called with enable: %d", enable);
    enableSdpHook.store(enable, std::memory_order_relaxed);

    LOGI("sdp hook enabled: %d", enable);
}
