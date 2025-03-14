<?php
date_default_timezone_set('Asia/Shanghai'); // 设置时区

// 日志记录函数
function logMessage($message) {
    $log_file = __DIR__ . '/access.log';
    $timestamp = date('Y-m-d H:i:s');
    $log_entry = "[$timestamp] $message\n";
    file_put_contents($log_file, $log_entry, FILE_APPEND);
}

// 验证Token
if (!isset($_GET['token']) || $_GET['token'] !== 'abc') {
    logMessage("Invalid token attempt");
    die("Invalid token");
}

$method = isset($_GET['method']) ? $_GET['method'] : '';
$get = isset($_GET['get']) ? $_GET['get'] : '';
$m = isset($_GET['m']) ? $_GET['m'] : '';

// 定义SQLite数据库文件路径
$database_file = __DIR__ . '/database.sqlite';

// 初始化数据库连接
try {
    $db = new PDO("sqlite:$database_file");
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    logMessage("Database connection failed: " . $e->getMessage());
    die("Database connection failed: " . $e->getMessage());
}

// 创建表结构（如果不存在）
try {
    $db->exec("CREATE TABLE IF NOT EXISTS orders (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        status TEXT DEFAULT '',
        order_number TEXT UNIQUE,
        type INTEGER,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )");
} catch (PDOException $e) {
    logMessage("Failed to create table: " . $e->getMessage());
    die("Failed to create table: " . $e->getMessage());
}

switch ($method) {
    case '1':
        handleMethod1($db);
        break;
    case '2':
        handleMethod2($db);
        break;
    case '3':
        handleMethod3($db);
        break;
    case 'c':
        handleMethodC($get);
        break;
    case 'get':
        handleMethodGet($get);
        break;
    case 'token':
        handleMethodToken($db);
        break;
    case 'time':
        handleMethodTime($db, $m);
        break;
    default:
        logMessage("Invalid method attempt: $method");
        die("Invalid method");
}

// 新增功能1：生成订单码
function handleMethodToken($db) {
    // 生成唯一订单号
    $order = generateOrder();
    $attempts = 0;
    $maxAttempts = 10;

    while ($attempts < $maxAttempts) {
        try {
            // 插入新记录（不包含type字段）
            $stmt = $db->prepare("INSERT INTO orders (order_number) VALUES (?)");
            $stmt->execute([$order]);
            logMessage("Token order created: $order");
            echo $order;
            return;
        } catch (PDOException $e) {
            if (strpos($e->getMessage(), 'UNIQUE constraint failed') !== false) {
                $order = generateOrder();
                $attempts++;
            } else {
                logMessage("Failed to insert token order: " . $e->getMessage());
                die("Failed to generate token");
            }
        }
    }
    logMessage("Failed to generate token after $maxAttempts attempts");
    die("Failed to generate token");
}

// 新增功能2：查询生成时间
function handleMethodTime($db, $order) {
    if (empty($order)) {
        logMessage("Missing order parameter in method=time");
        die("Missing order number");
    }

    try {
        $stmt = $db->prepare("SELECT created_at FROM orders WHERE order_number = ?");
        $stmt->execute([$order]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($row) {
            $created_at = strtotime($row['created_at']);
            $current_time = time();
            $minutes = floor(($current_time - $created_at) / 60);
            
            logMessage("Time query for order: $order ($minutes minutes)");
            echo "该订单码生成于 $minutes 分钟前";
        } else {
            logMessage("Time query failed: order not found - $order");
            echo "订单码不存在";
        }
    } catch (PDOException $e) {
        logMessage("Time query failed: " . $e->getMessage());
        die("查询失败");
    }
}

function handleMethod1($db) {
    // 验证参数
    if (!isset($_GET['type'])) {
        logMessage("Missing type parameter in method=1");
        die("Missing type parameter");
    }

    // 严格校验 type 参数
    $type = $_GET['type'];
    if (!ctype_digit($type)) { // 检查是否为纯数字
        logMessage("Invalid type parameter (not an integer): $type");
        die("Invalid type parameter: must be an integer");
    }

    $type = intval($type); // 转换为整数
    if ($type < 1 || $type > 100) { // 假设 type 的范围是 1 到 100
        logMessage("Invalid type parameter (out of range): $type");
        die("Invalid type parameter: must be between 1 and 100");
    }

    // 生成唯一订单号
    $order = generateOrder();
    $attempts = 0;
    $maxAttempts = 10; // 最大尝试次数，避免无限循环

    while ($attempts < $maxAttempts) {
        try {
            // 插入新记录
            $stmt = $db->prepare("INSERT INTO orders (order_number, type) VALUES (?, ?)");
            $stmt->execute([$order, $type]);
            logMessage("Order created: $order, type: $type");
            echo $order; // 直接输出订单号
            return;
        } catch (PDOException $e) {
            if (strpos($e->getMessage(), 'UNIQUE constraint failed') !== false) {
                // 订单号冲突，重新生成
                $order = generateOrder();
                $attempts++;
            } else {
                logMessage("Failed to insert order: " . $e->getMessage());
                die("Failed to insert order: " . $e->getMessage());
            }
        }
    }

    logMessage("Failed to generate unique order number after $maxAttempts attempts");
    die("Failed to generate unique order number after $maxAttempts attempts");
}

function handleMethod2($db) {
    try {
        $stmt = $db->query("SELECT * FROM orders ORDER BY id ASC");
        $orders = $stmt->fetchAll(PDO::FETCH_ASSOC);
        logMessage("Fetched all orders");
        echo json_encode(['status' => 'success', 'data' => $orders]);
    } catch (PDOException $e) {
        logMessage("Failed to fetch orders: " . $e->getMessage());
        die("Failed to fetch orders: " . $e->getMessage());
    }
}

function handleMethod3($db) {
    if (!isset($_GET['get'])) {
        logMessage("Missing order number in method=3");
        die("Missing order number");
    }

    $target = $_GET['get'];

    try {
        // 标记订单为完成
        $stmt = $db->prepare("UPDATE orders SET status = '#完成#' WHERE order_number = ?");
        $stmt->execute([$target]);

        if ($stmt->rowCount() > 0) {
            logMessage("Order marked as completed: $target");
            echo "Order marked as completed";
        } else {
            logMessage("Order not found: $target");
            echo "Order not found";
        }
    } catch (PDOException $e) {
        logMessage("Failed to update order: " . $e->getMessage());
        die("Failed to update order: " . $e->getMessage());
    }
}

function handleMethodC($get) {
    if ($get === 'all') {
        // 返回"套餐"文件内容
        $file_path = __DIR__ . '/套餐';
        if (file_exists($file_path)) {
            logMessage("Accessed file: 套餐");
            echo file_get_contents($file_path);
        } else {
            logMessage("File not found: 套餐");
            die("File '套餐' does not exist");
        }
    } elseif ($get === 'num') {
        // 返回"套餐个数"文件内容
        $file_path = __DIR__ . '/套餐个数';
        if (file_exists($file_path)) {
            logMessage("Accessed file: 套餐个数");
            echo file_get_contents($file_path);
        } else {
            logMessage("File not found: 套餐个数");
            die("File '套餐个数' does not exist");
        }
    } else {
        logMessage("Invalid 'get' parameter for method 'c': $get");
        die("Invalid 'get' parameter for method 'c'");
    }
}

function handleMethodGet($get) {
    if (empty($get)) {
        logMessage("Missing 'get' parameter for method 'get'");
        die("Missing 'get' parameter for method 'get'");
    }

    // 校验文件名，防止路径遍历攻击
    if (!preg_match('/^[a-zA-Z0-9_-]+$/', $get)) {
        logMessage("Invalid file name attempt: $get");
        die("Invalid file name");
    }

    // 返回"套餐X"文件内容
    $file_path = __DIR__ . '/套餐' . $get;
    if (file_exists($file_path)) {
        logMessage("Accessed file: 套餐$get");
        echo file_get_contents($file_path);
    } else {
        logMessage("File not found: 套餐$get");
        die("File '套餐$get' does not exist");
    }
}

function generateOrder() {
    // 结合时间戳和随机数生成订单号
    $timestamp = time(); // 当前时间戳
    $random = bin2hex(random_bytes(4)); // 8位随机字符串
    return strtoupper(dechex($timestamp)) . '-' . $random;
}
