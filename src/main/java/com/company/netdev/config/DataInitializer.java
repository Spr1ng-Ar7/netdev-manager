package com.company.netdev.config;

import com.company.netdev.common.PasswordUtil;
import com.company.netdev.entity.*;
import com.company.netdev.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 首次启动预置演示数据（仅当用户表为空时执行）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String IMAGE_API = "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=%s&image_size=square_hd";

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final DeviceRepository deviceRepository;
    private final ImageRepository imageRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final LoanRepository loanRepository;
    private final InspectionRepository inspectionRepository;
    private final OperationLogRepository logRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("====== 初始化演示数据 ======");

        // ---------- 用户 ----------
        User admin = createUser("admin", "admin123", "系统管理员", "ADMIN", "13800000001", "admin@company.com");
        User zhangwei = createUser("zhangwei", "zhang123", "张伟", "USER", "13800000002", "zhangwei@company.com");
        User liuna = createUser("liuna", "liu123", "刘娜", "USER", "13800000003", "liuna@company.com");

        // ---------- 部门 / 位置 ----------
        Department tech = createDept("技术部", "负责网络与系统运维");
        Department market = createDept("市场部", "");
        Department finance = createDept("财务部", "");
        Department hr = createDept("人力资源部", "");
        Department adminDept = createDept("行政部", "");

        Location room1 = createLoc("一号机房", "办公楼 3 层");
        Location room2 = createLoc("二号机房", "办公楼 1 层");
        Location officeA = createLoc("办公区 A", "5 层东区");
        Location officeB = createLoc("办公区 B", "5 层西区");
        Location warehouse = createLoc("设备仓库", "地下一层");

        // ---------- 设备（购买日期分布在近 12 个月） ----------
        LocalDate today = LocalDate.now();
        List<Device> devices = new ArrayList<>();
        devices.add(createDevice("核心路由器-01", "路由器", "华为", "AR6280", "10.0.0.1", "00:11:22:33:44:01", "SN-R2026-0001", "IN_USE", "张伟", tech, room1, today.minusMonths(11), today.plusYears(2)));
        devices.add(createDevice("核心交换机-01", "交换机", "华为", "S6730-H24X6C", "10.0.0.2", "00:11:22:33:44:02", "SN-S2026-0002", "IN_USE", "张伟", tech, room1, today.minusMonths(10), today.plusYears(2)));
        devices.add(createDevice("接入交换机-A01", "交换机", "H3C", "S5130S-28P", "10.0.1.11", "00:11:22:33:44:03", "SN-S2026-0003", "IN_USE", "张伟", market, officeA, today.minusMonths(9), today.plusYears(1)));
        devices.add(createDevice("接入交换机-B01", "交换机", "H3C", "S5130S-28P", "10.0.2.11", "00:11:22:33:44:04", "SN-S2026-0004", "IDLE", "刘娜", adminDept, warehouse, today.minusMonths(9), today.plusYears(1)));
        devices.add(createDevice("互联网防火墙", "防火墙", "深信服", "AF-2000", "10.0.0.254", "00:11:22:33:44:05", "SN-F2026-0005", "IN_USE", "张伟", tech, room1, today.minusMonths(8), today.plusYears(2)));
        devices.add(createDevice("无线AP-5F东区", "无线AP", "锐捷", "RG-AP850-I", "10.0.1.51", "00:11:22:33:44:06", "SN-W2026-0006", "IN_USE", "刘娜", market, officeA, today.minusMonths(7), today.plusYears(1)));
        devices.add(createDevice("无线AP-5F西区", "无线AP", "锐捷", "RG-AP850-I", "10.0.2.51", "00:11:22:33:44:07", "SN-W2026-0007", "FAULT", "刘娜", finance, officeB, today.minusMonths(7), today.plusYears(1)));
        devices.add(createDevice("OA应用服务器", "服务器", "戴尔", "PowerEdge R750", "10.0.10.5", "00:11:22:33:44:08", "SN-V2026-0008", "IN_USE", "张伟", tech, room1, today.minusMonths(6), today.plusYears(3)));
        devices.add(createDevice("数据库服务器", "服务器", "戴尔", "PowerEdge R750", "10.0.10.6", "00:11:22:33:44:09", "SN-V2026-0009", "IN_USE", "张伟", tech, room1, today.minusMonths(6), today.plusYears(3)));
        devices.add(createDevice("负载均衡-01", "负载均衡", "F5", "BIG-IP i2600", "10.0.0.100", "00:11:22:33:44:10", "SN-L2026-0010", "IN_USE", "张伟", tech, room2, today.minusMonths(5), today.plusYears(2)));
        devices.add(createDevice("机房UPS-01", "UPS", "山特", "C10KS", null, null, "SN-U2026-0011", "IN_USE", "刘娜", adminDept, room1, today.minusMonths(4), today.plusYears(2)));
        devices.add(createDevice("仓库监控摄像头", "摄像头", "海康威视", "DS-2CD3T46", "10.0.20.31", "00:11:22:33:44:12", "SN-C2026-0012", "IN_USE", "刘娜", adminDept, warehouse, today.minusMonths(3), today.plusYears(1)));
        devices.add(createDevice("备用路由器-02", "路由器", "思科", "ISR4331", "10.0.0.3", "00:11:22:33:44:13", "SN-R2026-0013", "IDLE", "张伟", tech, warehouse, today.minusMonths(2), today.plusYears(2)));
        devices.add(createDevice("测试服务器", "服务器", "联想", "ThinkSystem SR650", "10.0.10.20", "00:11:22:33:44:14", "SN-V2026-0014", "REPAIR", "张伟", tech, room2, today.minusMonths(1), today.plusYears(2)));
        devices.add(createDevice("旧交换机-2019", "交换机", "华为", "S5700-28P", "10.0.9.9", "00:11:22:33:44:15", "SN-S2019-0015", "RETIRED", "刘娜", adminDept, warehouse, today.minusMonths(12), today.minusMonths(1)));
        devices.add(createDevice("前台摄像头", "摄像头", "海康威视", "DS-2CD3346", "10.0.20.32", "00:11:22:33:44:16", "SN-C2026-0016", "LOANED", "刘娜", hr, officeA, today.minusMonths(0), today.plusYears(1)));

        // ---------- 种子图片（调用图片生成接口，失败则跳过） ----------
        String[] prompts = {
                "enterprise network router device, rack-mounted router with antennas, professional product photo, clean light gray background, soft studio lighting",
                "enterprise 24-port network switch in rack, ethernet cables, professional product photo, light gray background",
                "enterprise hardware firewall appliance, security network device, professional product photo, light gray background",
                "white ceiling mount wireless access point wifi AP device, professional product photo, light gray background",
                "2U rack server enterprise hardware, professional product photo, light gray background",
                "black tower UPS uninterruptible power supply device, professional product photo, light gray background"
        };
        downloadSeedImages(prompts, devices);

        // ---------- 维保工单 ----------
        MaintenanceTicket m1 = new MaintenanceTicket();
        m1.setDevice(devices.get(6)); // 无线AP-5F西区 FAULT
        m1.setApplicant(liuna);
        m1.setFaultDesc("AP 频繁掉线，5F 西区无线信号不稳定，重启后短暂恢复");
        m1.setStatus("PENDING");
        maintenanceRepository.save(m1);

        MaintenanceTicket m2 = new MaintenanceTicket();
        m2.setDevice(devices.get(13)); // 测试服务器 REPAIR
        m2.setApplicant(zhangwei);
        m2.setFaultDesc("服务器无法开机，电源指示灯不亮，疑似电源模块故障");
        m2.setStatus("PROCESSING");
        m2.setHandler(admin);
        m2.setAcceptTime(LocalDateTime.now().minusDays(2));
        maintenanceRepository.save(m2);

        MaintenanceTicket m3 = new MaintenanceTicket();
        m3.setDevice(devices.get(1)); // 核心交换机
        m3.setApplicant(zhangwei);
        m3.setFaultDesc("端口 1/0/12 协商异常，更换光模块后恢复");
        m3.setStatus("DONE");
        m3.setHandler(admin);
        m3.setAcceptTime(LocalDateTime.now().minusDays(20));
        m3.setResult("更换光模块并清洁光纤接头，端口恢复正常");
        m3.setCost(new BigDecimal("850.00"));
        m3.setFinishTime(LocalDateTime.now().minusDays(18));
        m3.setApplyTime(LocalDateTime.now().minusDays(21));
        maintenanceRepository.save(m3);

        MaintenanceTicket m4 = new MaintenanceTicket();
        m4.setDevice(devices.get(11)); // 仓库摄像头
        m4.setApplicant(liuna);
        m4.setFaultDesc("画面偶尔模糊，怀疑镜头脏污");
        m4.setStatus("REJECTED");
        m4.setHandler(admin);
        m4.setAcceptTime(LocalDateTime.now().minusDays(10));
        m4.setResult("现场检查为网络抖动导致，非设备故障，驳回维保申请");
        m4.setApplyTime(LocalDateTime.now().minusDays(11));
        maintenanceRepository.save(m4);

        // ---------- 借还记录 ----------
        LoanRecord l1 = new LoanRecord();
        l1.setDevice(devices.get(15)); // 前台摄像头 LOANED
        l1.setApplicant(liuna);
        l1.setBorrower("人力资源部-王芳");
        l1.setPurpose("新员工培训教室临时监控");
        l1.setExpectReturnDate(today.minusDays(3)); // 已超期
        l1.setStatus("APPROVED");
        l1.setApprover(admin);
        l1.setApproveTime(LocalDateTime.now().minusDays(15));
        l1.setApplyTime(LocalDateTime.now().minusDays(16));
        loanRepository.save(l1);

        LoanRecord l2 = new LoanRecord();
        l2.setDevice(devices.get(3)); // 接入交换机-B01 IDLE
        l2.setApplicant(zhangwei);
        l2.setBorrower("市场部-李强");
        l2.setPurpose("展会临时网络搭建");
        l2.setExpectReturnDate(today.plusDays(10));
        l2.setStatus("PENDING");
        loanRepository.save(l2);

        LoanRecord l3 = new LoanRecord();
        l3.setDevice(devices.get(12)); // 备用路由器
        l3.setApplicant(zhangwei);
        l3.setBorrower("分公司-赵敏");
        l3.setPurpose("分公司网络临时扩容");
        l3.setExpectReturnDate(today.minusDays(20));
        l3.setStatus("RETURNED");
        l3.setApprover(admin);
        l3.setApproveTime(LocalDateTime.now().minusDays(40));
        l3.setReturnDate(today.minusDays(20));
        l3.setReturnRemark("设备完好，配件齐全");
        l3.setApplyTime(LocalDateTime.now().minusDays(41));
        loanRepository.save(l3);

        // ---------- 巡检记录 ----------
        createInspection(devices.get(0), zhangwei, "NORMAL", "设备运行正常，CPU 利用率 35%，温度正常", LocalDateTime.now().minusDays(5));
        createInspection(devices.get(1), zhangwei, "NORMAL", "各端口流量正常，无告警", LocalDateTime.now().minusDays(5));
        createInspection(devices.get(10), liuna, "NORMAL", "UPS 电池状态良好，负载 42%", LocalDateTime.now().minusDays(3));
        createInspection(devices.get(6), liuna, "ABNORMAL", "AP 频繁掉线，已提交维保申请", LocalDateTime.now().minusDays(2));

        // ---------- 操作日志 ----------
        opLog(admin, "系统", "初始化", "系统初始化演示数据");
        opLog(admin, "认证", "登录", "用户登录系统");

        log.info("====== 演示数据初始化完成：用户 {} / 设备 {} ======", userRepository.count(), deviceRepository.count());
    }

    private User createUser(String username, String password, String name, String role, String phone, String email) {
        User user = new User();
        user.setUsername(username);
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        user.setSalt(salt);
        user.setPasswordHash(PasswordUtil.hash(password, salt));
        user.setName(name);
        user.setRole(role);
        user.setPhone(phone);
        user.setEmail(email);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private Department createDept(String name, String remark) {
        Department d = new Department();
        d.setName(name);
        d.setRemark(remark);
        return departmentRepository.save(d);
    }

    private Location createLoc(String name, String remark) {
        Location l = new Location();
        l.setName(name);
        l.setRemark(remark);
        return locationRepository.save(l);
    }

    private Device createDevice(String name, String type, String brand, String model, String ip, String mac,
                                String sn, String status, String manager, Department dept, Location loc,
                                LocalDate purchaseDate, LocalDate warrantyExpire) {
        Device d = new Device();
        d.setName(name);
        d.setType(type);
        d.setBrand(brand);
        d.setModel(model);
        d.setIp(ip);
        d.setMac(mac);
        d.setSerialNumber(sn);
        d.setStatus(status);
        d.setManager(manager);
        d.setDepartment(dept);
        d.setLocation(loc);
        d.setPurchaseDate(purchaseDate);
        d.setWarrantyExpire(warrantyExpire);
        return deviceRepository.save(d);
    }

    private void createInspection(Device device, User inspector, String result, String content, LocalDateTime time) {
        InspectionRecord r = new InspectionRecord();
        r.setDevice(device);
        r.setInspector(inspector);
        r.setResult(result);
        r.setContent(content);
        r.setInspectTime(time);
        inspectionRepository.save(r);
    }

    private void opLog(User user, String module, String action, String detail) {
        OperationLog l = new OperationLog();
        l.setUserId(user.getId());
        l.setUsername(user.getUsername());
        l.setModule(module);
        l.setAction(action);
        l.setDetail(detail);
        l.setIp("127.0.0.1");
        logRepository.save(l);
    }

    /** 下载生成的设备图片作为种子图片，挂到前 6 台设备上；失败则跳过不影响启动 */
    private void downloadSeedImages(String[] prompts, List<Device> devices) {
        try {
            Path dir = Paths.get("uploads").toAbsolutePath();
            Files.createDirectories(dir);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            int count = Math.min(prompts.length, devices.size());
            for (int i = 0; i < count; i++) {
                try {
                    String url = String.format(IMAGE_API,
                            URLEncoder.encode(prompts[i], StandardCharsets.UTF_8));
                    HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofSeconds(60)).GET().build();
                    HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                    if (resp.statusCode() != 200) {
                        log.warn("种子图片 {} 下载失败，HTTP {}", i + 1, resp.statusCode());
                        continue;
                    }
                    String stored = UUID.randomUUID().toString().replace("-", "") + ".jpg";
                    try (InputStream in = resp.body()) {
                        Files.copy(in, dir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
                    }
                    UploadImage img = new UploadImage();
                    img.setBizType("DEVICE");
                    img.setBizId(devices.get(i).getId());
                    img.setStoredName(stored);
                    img.setOriginalName("seed-" + (i + 1) + ".jpg");
                    img.setSize(Files.size(dir.resolve(stored)));
                    imageRepository.save(img);
                    log.info("种子图片 {} 已生成", i + 1);
                } catch (Exception e) {
                    log.warn("种子图片 {} 下载异常：{}", i + 1, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("种子图片目录创建失败：{}", e.getMessage());
        }
    }
}
