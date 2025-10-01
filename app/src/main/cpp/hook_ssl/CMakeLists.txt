cmake_minimum_required(VERSION 3.10.2)
project("ssl2")

find_library(
        log-lib
        log)

file(GLOB ssl_src
        "./*.cpp"
        "./*.h"
        "../third/utils/log.h"
        "../third/utils/log.cpp"
        "../third/utils/utils.h"
        "../third/utils/utils.cpp"
        "../third/dobby/include/dobby.h"
        )

add_library(
        ssl2
        SHARED
        ${ssl_src})

target_link_libraries(
        ssl2
        libutils
        libbyopen
        dobby
        ${log-lib})

set_target_properties(ssl2 PROPERTIES
        LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/../../libs/${ANDROID_ABI}")
