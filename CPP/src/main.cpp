#include <string>
#include <cstring>
#include <print>
#include <chrono>
#include <thread>
#include <csignal>
#include <atomic>

extern "C" {
#include "libyuanshen.h"
#include "libshared_api.h"
}

extern "C" {
    int __android_log_write(int, const char*, const char*) { return 0; }
    int __android_log_print(int, const char*, const char*, ...) { return 0; }
    int dladdr(int, const char*, const char*) { return 1; }
}

std::atomic<bool> shouldExit(false);
bool debug = false;

void signalHandler(int) { shouldExit = true; }

#define LOG(...) do { if (debug) std::println(__VA_ARGS__); } while(0)

bool testDownload() {
    std::string path = "/data/local/tmp/test.webp";
    bool success = GoDownloadFile((char *)"https://s3.yumeyuka.plus/2025/09/b90d3ab6c2b966bdbb84b1204597a23d.webp", (char *)path.c_str()) == 1;
    system(("rm -f " + path).c_str());
    return success;
}

void downloadAndInstall(const std::string &url, const std::string &filename, 
                        libshared_kref_YuanShen mgr, libshared_ExportedSymbols *sym) {
    std::string path = "/data/local/tmp/" + filename;
    
    if (GoDownloadFile((char *)url.c_str(), (char *)path.c_str()) == 1) {
        LOG("Downloaded");
        if (sym->kotlin.root.installGame(mgr, path.c_str())) {
            LOG("Installed");
        }
    }
    
    system(("rm -f " + path).c_str());
}

int main(int argc, char** argv) {
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "-debug") == 0) debug = true;
    }

    std::signal(SIGINT, signalHandler);
    std::signal(SIGTERM, signalHandler);

    auto sleep24h = []{ for (int i = 0; i < 288 && !shouldExit; i++) std::this_thread::sleep_for(std::chrono::minutes(5)); };
    auto sleep4h = []{ for (int i = 0; i < 48 && !shouldExit; i++) std::this_thread::sleep_for(std::chrono::minutes(5)); };

    while (!shouldExit) {
        auto sym = libshared_symbols();
        if (!sym) { sleep24h(); continue; }

        auto mgr = sym->kotlin.root.createYuanShen();
        if (!mgr.pinned) { sleep24h(); continue; }

        char *info = GoGetRedirectInfo((char *)"https://ys-api.mihoyo.com/event/download_porter/link/ys_cn/official/android_default");
        if (info && info[0]) {
            std::string s(info);
            size_t pos = s.find('|');
            
            if (pos != std::string::npos) {
                if (!testDownload()) {
                    sleep4h();
                    if (!shouldExit && !testDownload()) { sym->DisposeStablePointer(mgr.pinned); sleep24h(); continue; }
                }

                std::string url = s.substr(0, pos);
                std::string file = s.substr(pos + 1);
                bool needUpdate = false;
                std::string info;

                if (!sym->kotlin.root.isGameInstalledStatic()) {
                    needUpdate = true;
                    info = "New";
                } else {
                    auto cur = sym->kotlin.root.getGameVersionStatic();
                    auto apk = sym->kotlin.root.getApkVersionStatic(file.c_str());
                    if (cur && apk && sym->kotlin.root.compareVersion(mgr, cur, apk) == 2) {
                        needUpdate = true;
                        info = std::string(apk) + " > " + cur;
                    }
                }

                if (needUpdate) {
                    LOG("Update: {}", info);
                    downloadAndInstall(url, file, mgr, sym);
                }
            }
        }

        sym->DisposeStablePointer(mgr.pinned);
        sleep24h();
    }
}
