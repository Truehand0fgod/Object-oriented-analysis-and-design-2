#include <iostream>
#include <iomanip>
#include <string>
#include <vector>
#include <algorithm>
#include <filesystem>
#include <chrono>
#include <sstream>

namespace fs = std::filesystem;

// ============================================================================
// Вспомогательные функции (те же, что и в версии с паттерном)
// ============================================================================

std::string formatSize(uint64_t bytes) {
    const char* units[] = {"B", "KB", "MB", "GB", "TB"};
    int unitIndex = 0;
    double size = static_cast<double>(bytes);
    
    while (size >= 1024.0 && unitIndex < 4) {
        size /= 1024.0;
        unitIndex++;
    }
    
    std::ostringstream oss;
    if (unitIndex == 0) {
        oss << static_cast<uint64_t>(size) << " " << units[unitIndex];
    } else {
        oss << std::fixed << std::setprecision(1) << size << " " << units[unitIndex];
    }
    return oss.str();
}

uint64_t parseSize(const std::string& str) {
    if (str.empty()) return 0;
    
    uint64_t multiplier = 1;
    char lastChar = str.back();
    
    if (lastChar == 'K' || lastChar == 'k') {
        multiplier = 1024;
    } else if (lastChar == 'M' || lastChar == 'm') {
        multiplier = 1024 * 1024;
    } else if (lastChar == 'G' || lastChar == 'g') {
        multiplier = 1024 * 1024 * 1024;
    } else if (lastChar == 'T' || lastChar == 't') {
        multiplier = 1024ULL * 1024 * 1024 * 1024;
    }
    
    if (multiplier > 1) {
        return std::stoull(str.substr(0, str.length() - 1)) * multiplier;
    }
    
    return std::stoull(str);
}

// ============================================================================
// Структура данных БЕЗ использования паттерна Composite
// 
// ПРОБЛЕМА 1: Необходимо хранить флаг для различения типов
// ПРОБЛЕМА 2: Поля children и size используются по-разному в зависимости от типа
// ПРОБЛЕМА 3: У файла есть пустой вектор children (трата памяти)
// ПРОБЛЕМА 4: У папки поле size дублирует сумму детей (потенциальная несогласованность)
// ============================================================================

struct Node {
    std::string name;
    fs::path path;
    bool isDirectory;          // Флаг типа - необходимо проверять везде!
    uint64_t size;             // Для файлов: реальный размер, для папок: суммарный (кэш)
    std::vector<Node> children; // Для файлов: всегда пустой (трата памяти)
    
    Node(const fs::path& p, bool isDir) 
        : name(p.filename().string()), path(p), isDirectory(isDir), size(0) {
        if (name.empty()) name = path.string();
    }
};

// ============================================================================
// Функции для работы с файловой системой
// ============================================================================

// Получение размера файла (простая)
uint64_t getFileSize(const fs::path& path) {
    try {
        return fs::file_size(path);
    } catch (const fs::filesystem_error&) {
        return 0;
    }
}

// ПРОБЛЕМА 5: Функция getDirectorySize дублирует логику обхода,
// которая уже используется в buildTree. Приходится обходить дважды
// или мириться с дублированием кода.
uint64_t getDirectorySize(const fs::path& path, bool showHidden, uint64_t minSize) {
    uint64_t totalSize = 0;
    
    try {
        for (const auto& entry : fs::directory_iterator(path)) {
            if (!showHidden) {
                std::string filename = entry.path().filename().string();
                if (!filename.empty() && filename[0] == '.') continue;
            }
            
            try {
                if (fs::is_directory(entry.status())) {
                    uint64_t subDirSize = getDirectorySize(entry.path(), showHidden, minSize);
                    if (subDirSize >= minSize) {
                        totalSize += subDirSize;
                    }
                } else if (fs::is_regular_file(entry.status())) {
                    uint64_t fileSize = getFileSize(entry.path());
                    if (fileSize >= minSize) {
                        totalSize += fileSize;
                    }
                }
            } catch (const fs::filesystem_error& e) {
                std::cerr << "Warning: Cannot access " << entry.path() 
                          << " (" << e.what() << ")" << std::endl;
            }
        }
    } catch (const fs::filesystem_error& e) {
        std::cerr << "Warning: Cannot read directory " << path 
                  << " (" << e.what() << ")" << std::endl;
    }
    
    return totalSize;
}

// ============================================================================
// Построение дерева (рекурсивное)
// ПРОБЛЕМА 6: Функция buildTree и getDirectorySize имеют практически
// одинаковый код обхода, но не могут быть объединены из-за разных целей
// ============================================================================

Node buildTree(const fs::path& rootPath, bool showHidden, uint64_t minSize) {
    if (!fs::exists(rootPath)) {
        throw std::runtime_error("Path does not exist: " + rootPath.string());
    }
    
    if (!fs::is_directory(rootPath)) {
        throw std::runtime_error("Path is not a directory: " + rootPath.string());
    }
    
    Node root(rootPath, true);
    
    try {
        for (const auto& entry : fs::directory_iterator(rootPath)) {
            if (!showHidden) {
                std::string filename = entry.path().filename().string();
                if (!filename.empty() && filename[0] == '.') continue;
            }
            
            try {
                if (fs::is_directory(entry.status())) {
                    Node subDir = buildTree(entry.path(), showHidden, minSize);
                    if (subDir.size >= minSize) {
                        root.children.push_back(std::move(subDir));
                        root.size += root.children.back().size;
                    }
                } else if (fs::is_regular_file(entry.status())) {
                    Node file(entry.path(), false);
                    file.size = getFileSize(entry.path());
                    if (file.size >= minSize) {
                        root.children.push_back(std::move(file));
                        root.size += root.children.back().size;
                    }
                }
            } catch (const fs::filesystem_error& e) {
                std::cerr << "Warning: Cannot access " << entry.path() 
                          << " (" << e.what() << ")" << std::endl;
            }
        }
    } catch (const fs::filesystem_error& e) {
        std::cerr << "Warning: Cannot read directory " << rootPath 
                  << " (" << e.what() << ")" << std::endl;
    }
    
    return root;
}

// ============================================================================
// Функции вывода
// ПРОБЛЕМА 7: Во всех функциях вывода нужны проверки типа isDirectory
// Легко забыть обработать один из типов, и программа сломается
// ============================================================================

// ПРОБЛЕМА 8: Сортировка детей требует отдельной функции с проверкой типов
void sortChildrenBySize(Node& node) {
    std::sort(node.children.begin(), node.children.end(),
              [](const Node& a, const Node& b) {
                  return a.size > b.size;
              });
    
    for (auto& child : node.children) {
        if (child.isDirectory) {
            sortChildrenBySize(child);
        }
    }
}

// ПРОБЛЕМА 9: Функция printTree рекурсивная, но для файлов и папок
// логика разная. При добавлении нового типа (например, символьная ссылка)
// придется модифицировать эту функцию.
void printTree(const Node& node, int depth, bool isLast, const std::string& prefix) {
    std::cout << prefix;
    std::cout << (isLast ? "└── " : "├── ");
    
    // Разный вывод для файлов и папок
    if (node.isDirectory) {
        std::cout << node.name << "/ (" << formatSize(node.size) << ")";
    } else {
        std::cout << node.name << " (" << formatSize(node.size) << ")";
    }
    std::cout << std::endl;
    
    std::string newPrefix = prefix + (isLast ? "    " : "│   ");
    
    // Предварительная сортировка для вывода (самые большие сверху)
    std::vector<size_t> sortedIndices;
    for (size_t i = 0; i < node.children.size(); ++i) {
        sortedIndices.push_back(i);
    }
    std::sort(sortedIndices.begin(), sortedIndices.end(),
              [&node](size_t a, size_t b) {
                  return node.children[a].size > node.children[b].size;
              });
    
    for (size_t idx = 0; idx < sortedIndices.size(); ++idx) {
        bool childIsLast = (idx == sortedIndices.size() - 1);
        printTree(node.children[sortedIndices[idx]], depth + 1, childIsLast, newPrefix);
    }
}

// ПРОБЛЕМА 10: collectItems должна проверять тип и обрабатывать детей
// Если забыть про recursию для папок, топ-N будет неполным
void collectItems(const Node& node, std::vector<std::pair<uint64_t, std::string>>& items) {
    items.emplace_back(node.size, node.path.string());
    
    for (const auto& child : node.children) {
        // Рекурсивный вызов для всех детей (независимо от типа)
        collectItems(child, items);
    }
}

void printTopItems(const Node& root, int topCount) {
    std::vector<std::pair<uint64_t, std::string>> items;
    collectItems(root, items);
    
    std::sort(items.begin(), items.end(),
              [](const auto& a, const auto& b) { return a.first > b.first; });
    
    std::cout << "\nTop " << std::min(topCount, (int)items.size()) 
              << " largest items:" << std::endl;
    std::cout << "========================================" << std::endl;
    
    for (int i = 0; i < std::min(topCount, (int)items.size()); ++i) {
        std::cout << std::setw(4) << (i + 1) << ". ";
        std::cout << formatSize(items[i].first) << " - ";
        std::cout << items[i].second << std::endl;
    }
    
    if ((int)items.size() > topCount) {
        std::cout << "\n... and " << (items.size() - topCount) 
                  << " more items" << std::endl;
    }
}

void printSummary(const Node& root) {
    int fileCount = 0;
    int dirCount = 0;
    
    std::vector<std::pair<uint64_t, std::string>> items;
    collectItems(root, items);
    
    for (const auto& item : items) {
        // ПРОБЛЕМА 11: Чтобы определить тип элемента, нужно проверить
        // существование пути как директории - это дорогая операция
        if (fs::is_directory(item.second)) {
            dirCount++;
        } else {
            fileCount++;
        }
    }
    
    std::cout << "\nSummary:" << std::endl;
    std::cout << "========================================" << std::endl;
    std::cout << "Total size:   " << formatSize(root.size) << std::endl;
    std::cout << "Total files:  " << fileCount << std::endl;
    std::cout << "Total folders: " << dirCount << std::endl;
    std::cout << "Total items:  " << items.size() << std::endl;
}

// ============================================================================
// Парсинг аргументов командной строки (такой же, как в версии с паттерном)
// ============================================================================

void printUsage(const char* programName) {
    std::cout << "Disk Usage Analyzer (NO Composite pattern) - analyze disk space usage" << std::endl;
    std::cout << std::endl;
    std::cout << "Usage: " << programName << " [options] [path]" << std::endl;
    std::cout << std::endl;
    std::cout << "Options:" << std::endl;
    std::cout << "  --help, -h        Show this help message" << std::endl;
    std::cout << "  --tree, -t        Show tree view (default)" << std::endl;
    std::cout << "  --top N, -n N     Show top N largest items (default: 10)" << std::endl;
    std::cout << "  --min-size SIZE   Filter items smaller than SIZE" << std::endl;
    std::cout << "  --hidden, -a      Include hidden files and directories" << std::endl;
    std::cout << "  --summary, -s     Show only summary statistics" << std::endl;
    std::cout << "  --sort, -r        Sort output by size (used with --tree)" << std::endl;
    std::cout << std::endl;
    std::cout << "SIZE format: number with optional unit (K, M, G, T)" << std::endl;
    std::cout << "Examples: 100M, 2G, 512K" << std::endl;
    std::cout << std::endl;
    std::cout << "Examples:" << std::endl;
    std::cout << "  " << programName << " /home/user" << std::endl;
    std::cout << "  " << programName << " --top 20 --min-size 10M ~/Downloads" << std::endl;
}

struct CommandLineOptions {
    fs::path path = fs::current_path();
    bool showTree = true;
    bool showSummary = false;
    int topCount = 10;
    uint64_t minSize = 0;
    bool showHidden = false;
    bool sortBySize = false;
};

CommandLineOptions parseArguments(int argc, char* argv[]) {
    CommandLineOptions opts;
    
    for (int i = 1; i < argc; ++i) {
        std::string arg = argv[i];
        
        if (arg == "--help" || arg == "-h") {
            printUsage(argv[0]);
            exit(0);
        }
        else if ((arg == "--top" || arg == "-n") && i + 1 < argc) {
            opts.topCount = std::stoi(argv[++i]);
            opts.showTree = false;
        }
        else if ((arg == "--min-size") && i + 1 < argc) {
            opts.minSize = parseSize(argv[++i]);
        }
        else if (arg == "--hidden" || arg == "-a") {
            opts.showHidden = true;
        }
        else if (arg == "--summary" || arg == "-s") {
            opts.showSummary = true;
            opts.showTree = false;
        }
        else if (arg == "--sort" || arg == "-r") {
            opts.sortBySize = true;
        }
        else if (arg[0] != '-') {
            opts.path = arg;
        }
        else {
            std::cerr << "Unknown option: " << arg << std::endl;
            printUsage(argv[0]);
            exit(1);
        }
    }
    
    return opts;
}

// ============================================================================
// Основная функция
// ============================================================================

int main(int argc, char* argv[]) {
    try {
        CommandLineOptions opts = parseArguments(argc, argv);
        
        std::cout << "Disk Usage Analyzer (NO Composite pattern)" << std::endl;
        std::cout << "===========================================" << std::endl;
        std::cout << "Scanning: " << opts.path << std::endl;
        
        auto startTime = std::chrono::high_resolution_clock::now();
        
        Node root = buildTree(opts.path, opts.showHidden, opts.minSize);
        
        if (opts.sortBySize) {
            sortChildrenBySize(root);
        }
        
        auto endTime = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
        std::cout << "Scan completed in " << duration.count() << " ms" << std::endl;
        
        if (opts.showTree) {
            printTree(root, 0, true, "");
        }
        
        if (opts.showSummary) {
            printSummary(root);
        } else {
            printTopItems(root, opts.topCount);
            printSummary(root);
        }
        
        return 0;
        
    } catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << std::endl;
        return 1;
    }
}