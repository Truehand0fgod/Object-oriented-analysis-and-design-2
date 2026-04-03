#include <iostream>
#include <iomanip>
#include <string>
#include <vector>
#include <memory>
#include <algorithm>
#include <filesystem>
#include <chrono>

namespace fs = std::filesystem;

// Форматирование размера в человекочитаемый вид (B, KB, MB, GB)
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

// Парсинг строки размера
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
// Паттерн Composite: Абстрактный компонент
// ============================================================================

class FileSystemNode {
protected:
    std::string name_;
    fs::path path_;
    
public:
    FileSystemNode(const fs::path& path) 
        : name_(path.filename().string()), path_(path) {
        if (name_.empty()) name_ = path.string();
    }
    
    virtual ~FileSystemNode() = default;
    
    // Основные методы, демонстрирующие единообразный интерфейс для файлов и папок
    virtual uint64_t getSize() const = 0;
    virtual void print(int depth = 0, bool isLast = true, 
                       const std::string& prefix = "") const = 0;
    
    // Методы для композита (для листьев имеют пустую реализацию по умолчанию)
    virtual void addChild(std::unique_ptr<FileSystemNode> child) {
        // По умолчанию ничего не делаем (для файлов)
    }
    
    virtual const std::vector<std::unique_ptr<FileSystemNode>>& getChildren() const {
        static const std::vector<std::unique_ptr<FileSystemNode>> empty;
        return empty;
    }
    
    virtual void sortBySize() {
        // По умолчанию ничего не делаем
    }
    
    // Геттеры
    std::string getName() const { return name_; }
    const fs::path& getPath() const { return path_; }
    
    // Для сбора всех элементов (используется для топ-N)
    virtual void collectItems(std::vector<std::pair<uint64_t, const FileSystemNode*>>& items) const {
        // Для файлов добавляем себя
    }
};

// ============================================================================
// Паттерн Composite: Лист (Файл)
// ============================================================================

class File : public FileSystemNode {
    uint64_t size_;
    
public:
    File(const fs::path& path) 
        : FileSystemNode(path) {
        try {
            size_ = fs::file_size(path);
        } catch (const fs::filesystem_error&) {
            size_ = 0; // Не удалось получить размер (например, нет доступа)
        }
    }
    
    uint64_t getSize() const override {
        return size_;
    }
    
    void print(int depth, bool isLast, const std::string& prefix) const override {
        std::cout << prefix;
        std::cout << (isLast ? "└── " : "├── ");
        std::cout << name_;
        
        // Выводим размер файла
        std::cout << " (" << formatSize(size_) << ")";
        std::cout << std::endl;
    }
    
    void collectItems(std::vector<std::pair<uint64_t, const FileSystemNode*>>& items) const override {
        items.emplace_back(size_, this);
    }
};

// ============================================================================
// Паттерн Composite: Композит (Папка)
// ============================================================================

class Directory : public FileSystemNode {
    std::vector<std::unique_ptr<FileSystemNode>> children_;
    mutable uint64_t cachedSize_;
    mutable bool sizeCalculated_;
    
public:
    Directory(const fs::path& path) 
        : FileSystemNode(path), cachedSize_(0), sizeCalculated_(false) {}
    
    void addChild(std::unique_ptr<FileSystemNode> child) override {
        children_.push_back(std::move(child));
        sizeCalculated_ = false; // Сброс кэша при изменении структуры
    }
    
    uint64_t getSize() const override {
        if (!sizeCalculated_) {
            cachedSize_ = 0;
            for (const auto& child : children_) {
                cachedSize_ += child->getSize();
            }
            sizeCalculated_ = true;
        }
        return cachedSize_;
    }
    
    void print(int depth, bool isLast, const std::string& prefix) const override {
        // Выводим текущую папку
        std::cout << prefix;
        std::cout << (isLast ? "└── " : "├── ");
        
        // Используем разные иконки для наглядности (работает в современных терминалах)
        std::cout << name_;
        std::cout << " (" << formatSize(getSize()) << ")";
        std::cout << std::endl;
        
        // Формируем новый префикс для детей
        std::string newPrefix = prefix + (isLast ? "    " : "│   ");
        
        // Выводим детей (предварительно отсортированных)
        // Создаем копию для сортировки, чтобы не нарушать оригинальный порядок
        std::vector<std::pair<uint64_t, size_t>> sortedIndices;
        for (size_t i = 0; i < children_.size(); ++i) {
            sortedIndices.emplace_back(children_[i]->getSize(), i);
        }
        std::sort(sortedIndices.begin(), sortedIndices.end(), 
                  [](const auto& a, const auto& b) { return a.first > b.first; });
        
        for (size_t idx = 0; idx < sortedIndices.size(); ++idx) {
            size_t childIndex = sortedIndices[idx].second;
            bool childIsLast = (idx == sortedIndices.size() - 1);
            children_[childIndex]->print(depth + 1, childIsLast, newPrefix);
        }
    }
    
    void sortBySize() override {
        std::sort(children_.begin(), children_.end(),
                  [](const auto& a, const auto& b) {
                      return a->getSize() > b->getSize();
                  });
        
        // Рекурсивно сортируем детей
        for (auto& child : children_) {
            child->sortBySize();
        }
    }
    
    void collectItems(std::vector<std::pair<uint64_t, const FileSystemNode*>>& items) const override {
        items.emplace_back(getSize(), this);
        for (const auto& child : children_) {
            child->collectItems(items);
        }
    }
    
    const std::vector<std::unique_ptr<FileSystemNode>>& getChildren() const override {
        return children_;
    }
};

// ============================================================================
// Класс для сканирования файловой системы и построения дерева
// ============================================================================

class FileSystemScanner {
public:
    // Сканирование директории и построение Composite структуры
    static std::unique_ptr<Directory> scan(const fs::path& rootPath, 
                                            bool showHidden = false,
                                            uint64_t minSize = 0) {
        if (!fs::exists(rootPath)) {
            throw std::runtime_error("Path does not exist: " + rootPath.string());
        }
        
        if (!fs::is_directory(rootPath)) {
            throw std::runtime_error("Path is not a directory: " + rootPath.string());
        }
        
        auto root = std::make_unique<Directory>(rootPath);
        
        try {
            for (const auto& entry : fs::directory_iterator(rootPath)) {
                // Пропускаем скрытые файлы/папки если нужно
                if (!showHidden) {
                    std::string filename = entry.path().filename().string();
                    if (!filename.empty() && filename[0] == '.') {
                        continue;
                    }
                }
                
                try {
                    if (fs::is_directory(entry.status())) {
                        auto subDir = scan(entry.path(), showHidden, minSize);
                        if (subDir && subDir->getSize() >= minSize) {
                            root->addChild(std::move(subDir));
                        }
                    } else if (fs::is_regular_file(entry.status())) {
                        auto file = std::make_unique<File>(entry.path());
                        if (file->getSize() >= minSize) {
                            root->addChild(std::move(file));
                        }
                    }
                } catch (const fs::filesystem_error& e) {
                    // Пропускаем элементы, к которым нет доступа
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
};

// ============================================================================
// Класс для вывода отчета
// ============================================================================

class ReportGenerator {
public:
    // Вывод древовидной структуры
    static void printTree(const Directory& root, int maxDepth = -1) {
        std::cout << root.getName() << "/" << std::endl;
        
        // Получаем детей и выводим их
        const auto& children = root.getChildren();
        for (size_t i = 0; i < children.size(); ++i) {
            bool isLast = (i == children.size() - 1);
            children[i]->print(0, isLast, "");
        }
    }
    
    // Вывод топ-N самых больших элементов
    static void printTopItems(const Directory& root, int topCount) {
        std::vector<std::pair<uint64_t, const FileSystemNode*>> items;
        root.collectItems(items);
        
        std::sort(items.begin(), items.end(),
                  [](const auto& a, const auto& b) { return a.first > b.first; });
        
        std::cout << "\nTop " << std::min(topCount, (int)items.size()) 
                  << " largest items:" << std::endl;
        std::cout << "========================================" << std::endl;
        
        for (int i = 0; i < std::min(topCount, (int)items.size()); ++i) {
            std::cout << std::setw(4) << (i + 1) << ". ";
            std::cout << formatSize(items[i].first) << " - ";
            std::cout << items[i].second->getPath().string() << std::endl;
        }
        
        // Показываем общее количество элементов
        if ((int)items.size() > topCount) {
            std::cout << "\n... and " << (items.size() - topCount) 
                      << " more items" << std::endl;
        }
    }
    
    // Вывод сводной статистики
    static void printSummary(const Directory& root) {
        uint64_t totalSize = root.getSize();
        int fileCount = 0;
        int dirCount = 0;
        
        std::vector<std::pair<uint64_t, const FileSystemNode*>> items;
        root.collectItems(items);
        
        for (const auto& item : items) {
            if (dynamic_cast<const Directory*>(item.second)) {
                dirCount++;
            } else {
                fileCount++;
            }
        }
        
        std::cout << "\nSummary:" << std::endl;
        std::cout << "========================================" << std::endl;
        std::cout << "Total size:   " << formatSize(totalSize) << std::endl;
        std::cout << "Total files:  " << fileCount << std::endl;
        std::cout << "Total folders: " << dirCount << std::endl;
        std::cout << "Total items:  " << items.size() << std::endl;
    }
};


void printUsage(const char* programName) {
    std::cout << "Disk Usage Analyzer - analyze disk space usage" << std::endl;
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


int main(int argc, char* argv[]) {
    try {
        // Парсим аргументы командной строки
        CommandLineOptions opts = parseArguments(argc, argv);
        
        std::cout << "Disk Usage Analyzer" << std::endl;
        std::cout << "===================" << std::endl;
        std::cout << "Scanning: " << opts.path << std::endl;
        
        // Засекаем время
        auto startTime = std::chrono::high_resolution_clock::now();
        
        // Сканируем файловую систему и строим Composite дерево
        auto root = FileSystemScanner::scan(opts.path, opts.showHidden, opts.minSize);
        
        // Применяем сортировку если нужно
        if (opts.sortBySize) {
            root->sortBySize();
        }
        
        // Время сканирования
        auto endTime = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
        std::cout << "Scan completed in " << duration.count() << " ms" << std::endl;
        
        // Выводим результаты
        if (opts.showTree) {
            ReportGenerator::printTree(*root);
        }
        
        if (opts.showSummary) {
            ReportGenerator::printSummary(*root);
        } else {
            // Показываем топ элементов всегда, если не указан summary
            ReportGenerator::printTopItems(*root, opts.topCount);
            ReportGenerator::printSummary(*root);
        }
        
        return 0;
        
    } catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << std::endl;
        return 1;
    }
}