#include <android/log.h>
#include <cstring>
#include <string>
#include <vector>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <elf.h>
#include <jni.h>


extern "C" {
    #include "xz.h"
}

#define LOG_TAG "LibrePodsHook"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool decompressXZ(const uint8_t *input, size_t input_size, std::vector<uint8_t> &output) {

    LOGI("decompressXZ called with input_size: %zu", input_size);

    xz_crc32_init();
#ifdef XZ_USE_CRC64
    xz_crc64_init();
#endif

    struct xz_dec *dec = xz_dec_init(XZ_DYNALLOC, 64U << 20);
    if (!dec) {
        LOGE("decompressXZ: xz_dec_init failed");
        return false;
    }
    LOGI("decompressXZ: xz_dec_init succeeded");

    struct xz_buf buf{};
    buf.in = input;
    buf.in_pos = 0;
    buf.in_size = input_size;

    output.resize(input_size * 8);

    buf.out = output.data();
    buf.out_pos = 0;
    buf.out_size = output.size();

    LOGI("decompressXZ: entering decompression loop");
    while (true) {
        LOGI("decompressXZ: xz_dec_run iteration, buf.in_pos: %zu, buf.out_pos: %zu", buf.in_pos,
             buf.out_pos);
        enum xz_ret ret = xz_dec_run(dec, &buf);

        LOGI("decompressXZ: xz_dec_run returned %d", ret);

        if (ret == XZ_STREAM_END)
            break;

        if (ret != XZ_OK) {
            LOGE("decompressXZ: xz_dec_run error");
            xz_dec_end(dec);
            return false;
        }

        if (buf.out_pos == buf.out_size) {
            size_t old = output.size();
            LOGI("decompressXZ: resizing output to %zu", old * 2);
            output.resize(old * 2);
            buf.out = output.data();
            buf.out_size = output.size();
        }
    }

    output.resize(buf.out_pos);
    xz_dec_end(dec);
    LOGI("decompressXZ: decompression successful, output size: %zu", output.size());
    return true;
}

static bool getLibraryPath(const char *name, std::string &out) {
    LOGI("getLibraryPath called with name: %s", name);

    FILE *fp = fopen("/proc/self/maps", "r");
    if (!fp) {
        LOGE("getLibraryPath: fopen failed");
        return false;
    }

    char line[1024];

    LOGI("getLibraryPath: scanning /proc/self/maps");
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, name)) {
            LOGI("getLibraryPath: found line containing %s", name);
            char *path = strchr(line, '/');
            if (path) {
                out = path;
                out.erase(out.find('\n'));
                LOGI("getLibraryPath: path found: %s", out.c_str());
                fclose(fp);
                return true;
            }
        }
    }

    fclose(fp);
    LOGI("getLibraryPath: failed to find path for %s", name);
    return false;
}

static uintptr_t getModuleBase(const char *name) {
    LOGI("getModuleBase called with name: %s", name);

    FILE *fp = fopen("/proc/self/maps", "r");
    if (!fp) {
        LOGE("getModuleBase: fopen failed");
        return 0;
    }

    char line[1024];
    uintptr_t base = 0;

    LOGI("getModuleBase: scanning /proc/self/maps");
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, name)) {
            base = strtoull(line, nullptr, 16);
            LOGI("getModuleBase: found base at 0x%x", base);
            break;
        }
    }

    fclose(fp);
    LOGI("getModuleBase: failed to find base for %s", name);
    return base;
}

static uint64_t
findSymbolOffsetDynsym(const std::vector<uint8_t> &elf, const char *symbol_substring) {

    LOGI("findSymbolOffsetDynsym called with %s", symbol_substring);

    auto *eh = reinterpret_cast<const Elf64_Ehdr *>(elf.data());
    auto *shdr = reinterpret_cast<const Elf64_Shdr *>(
            elf.data() + eh->e_shoff);

    const char *shstr = reinterpret_cast<const char *>(
            elf.data() + shdr[eh->e_shstrndx].sh_offset);

    const Elf64_Shdr *dynsym = nullptr;
    const Elf64_Shdr *dynstr = nullptr;

    for (int i = 0; i < eh->e_shnum; ++i) {
        const char *secname = shstr + shdr[i].sh_name;

        if (!strcmp(secname, ".dynsym"))
            dynsym = &shdr[i];
        if (!strcmp(secname, ".dynstr"))
            dynstr = &shdr[i];
    }

    if (!dynsym || !dynstr) {
        LOGE("findSymbolOffsetDynsym: dynsym or dynstr not found");
        return 0;
    }

    auto *symbols = reinterpret_cast<const Elf64_Sym *>(
            elf.data() + dynsym->sh_offset);

    const char *strings = reinterpret_cast<const char *>(
            elf.data() + dynstr->sh_offset);

    size_t count = dynsym->sh_size / sizeof(Elf64_Sym);

    LOGI("findSymbolOffsetDynsym: scanning %zu symbols", count);

    for (size_t i = 0; i < count; ++i) {
        const char *name = strings + symbols[i].st_name;

        if (strstr(name, symbol_substring) && ELF64_ST_TYPE(symbols[i].st_info) == STT_FUNC) {

            LOGI("findSymbolOffsetDynsym: matched %s @ 0x%lx", name,
                 (unsigned long) symbols[i].st_value);

            return symbols[i].st_value;
        }
    }

    LOGI("findSymbolOffsetDynsym: no match for %s", symbol_substring);
    return 0;
}

static uint64_t findSymbolOffset(const std::vector<uint8_t> &elf, const char *symbol_substring) {

    LOGI("findSymbolOffset called with symbol_substring: %s", symbol_substring);

    auto *eh = reinterpret_cast<const Elf64_Ehdr *>(elf.data());
    auto *shdr = reinterpret_cast<const Elf64_Shdr *>(
            elf.data() + eh->e_shoff);

    const char *shstr = reinterpret_cast<const char *>(
            elf.data() + shdr[eh->e_shstrndx].sh_offset);

    const Elf64_Shdr *symtab = nullptr;
    const Elf64_Shdr *strtab = nullptr;

    LOGI("findSymbolOffset: parsing ELF sections");
    for (int i = 0; i < eh->e_shnum; ++i) {
        const char *secname = shstr + shdr[i].sh_name;
        if (!strcmp(secname, ".symtab"))
            symtab = &shdr[i];
        if (!strcmp(secname, ".strtab"))
            strtab = &shdr[i];
    }

    if (!symtab || !strtab) {
        LOGE("findSymbolOffset: symtab or strtab not found");
        return 0;
    }
    LOGI("findSymbolOffset: found symtab and strtab");

    auto *symbols = reinterpret_cast<const Elf64_Sym *>(
            elf.data() + symtab->sh_offset);

    const char *strings = reinterpret_cast<const char *>(
            elf.data() + strtab->sh_offset);

    size_t count = symtab->sh_size / sizeof(Elf64_Sym);

    LOGI("findSymbolOffset: scanning %zu symbols", count);
    for (size_t i = 0; i < count; ++i) {
        const char *name = strings + symbols[i].st_name;

        if (strstr(name, symbol_substring) && ELF64_ST_TYPE(symbols[i].st_info) == STT_FUNC) {

            LOGI("findSymbolOffset: matched symbol %s at 0x%lx", name,
                 (unsigned long) symbols[i].st_value);

            return symbols[i].st_value;
        }
    }

    LOGI("findSymbolOffset: no match found for %s", symbol_substring);
    return 0;
}
