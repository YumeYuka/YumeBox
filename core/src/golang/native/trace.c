// Copyright (c) YumeYuka 2025.
//
// This work is free. You can redistribute it and/or modify it under the
// terms of the Do What The Fuck You Want To Public License, Version 2,
//  as published by Sam Hocevar. See http://www.wtfpl.net/ for more details.

#include "trace.h"

#if ENABLE_TRACE

void trace_method_exit(const char **name) {
    __android_log_print(ANDROID_LOG_VERBOSE, TAG, "TRACE-OUT %s", *name);
}

#endif
