cmake_minimum_required(VERSION 3.10)
project(DiskAnalyzer VERSION 1.0.0)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(CMAKE_CXX_COMPILER_ID STREQUAL "GNU" AND CMAKE_CXX_COMPILER_VERSION VERSION_LESS 9.0)
    target_link_libraries(disk_analyzer stdc++fs)
endif()

add_executable(disk_analyzer main.cpp)

if(MSVC)
    target_compile_options(disk_analyzer PRIVATE /std:c++17)
endif()