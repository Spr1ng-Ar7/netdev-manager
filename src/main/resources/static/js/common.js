/* ============ 公共工具 ============ */

/** 统一请求封装：返回 data；code=401 跳登录页 */
async function request(url, options = {}) {
    const opts = { method: options.method || 'GET', headers: {}, credentials: 'same-origin' };
    if (options.body !== undefined) {
        opts.headers['Content-Type'] = 'application/json';
        opts.body = JSON.stringify(options.body);
    }
    const resp = await fetch(url, opts);
    let result;
    try { result = await resp.json(); } catch (e) { throw new Error('响应解析失败'); }
    if (result.code === 401) {
        location.href = 'login.html';
        throw new Error(result.msg || '未登录');
    }
    if (result.code !== 0) {
        const err = new Error(result.msg || '操作失败');
        err.code = result.code;
        throw err;
    }
    return result.data;
}

/** 上传文件 */
async function uploadFile(file, bizType, bizId) {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('bizType', bizType);
    fd.append('bizId', bizId);
    const resp = await fetch('/api/images/upload', { method: 'POST', body: fd, credentials: 'same-origin' });
    const result = await resp.json();
    if (result.code === 401) { location.href = 'login.html'; throw new Error('未登录'); }
    if (result.code !== 0) throw new Error(result.msg || '上传失败');
    return result.data;
}

/** 登录守卫 + 渲染布局 */
let currentUser = null;

async function authGuard(pageKey) {
    try {
        currentUser = await request('/api/auth/me');
    } catch (e) {
        return;
    }
    renderSidebar(pageKey);
    renderTopbar();
}

const MENUS = [
    { key: 'index', title: '统计仪表盘', icon: '📊', href: 'index.html' },
    { key: 'devices', title: '设备管理', icon: '🖥️', href: 'devices.html' },
    { key: 'maintenance', title: '维保维修', icon: '🔧', href: 'maintenance.html' },
    { key: 'loans', title: '借出归还', icon: '📦', href: 'loans.html' },
    { key: 'inspections', title: '巡检记录', icon: '📋', href: 'inspections.html' },
    { key: 'departments', title: '部门与位置', icon: '🏢', href: 'departments.html' },
    { key: 'logs', title: '操作日志', icon: '📝', href: 'logs.html', admin: true },
    { key: 'users', title: '用户管理', icon: '👥', href: 'users.html', admin: true },
];

function renderSidebar(activeKey) {
    const el = document.getElementById('sidebar');
    if (!el) return;
    const isAdmin = currentUser.role === 'ADMIN';
    const items = MENUS.filter(m => !m.admin || isAdmin);
    el.innerHTML = `
        <div class="logo"><span class="logo-icon">🌐</span><span>网络设备管理</span></div>
        <nav>${items.map(m => `
            <a href="${m.href}" class="${m.key === activeKey ? 'active' : ''}">
                <span class="icon">${m.icon}</span><span>${m.title}</span>
            </a>`).join('')}
        </nav>
        <div class="sidebar-footer">NetDev Manager v1.0</div>`;
}

function renderTopbar() {
    const el = document.getElementById('topbar');
    if (!el) return;
    const menu = MENUS.find(m => location.pathname.endsWith(m.href));
    el.innerHTML = `
        <div class="page-title">${menu ? menu.title : ''}</div>
        <div class="user-info">
            <span class="avatar">${(currentUser.name || currentUser.username).charAt(0)}</span>
            <span>${currentUser.name}（${currentUser.role === 'ADMIN' ? '管理员' : '普通用户'}）</span>
            <button class="btn btn-plain btn-sm" onclick="logout()">退出</button>
        </div>`;
}

async function logout() {
    try { await request('/api/auth/logout', { method: 'POST' }); } catch (e) { /* ignore */ }
    location.href = 'login.html';
}

/* ============ Toast ============ */
function toast(msg, type = 'info') {
    let wrap = document.querySelector('.toast-wrap');
    if (!wrap) {
        wrap = document.createElement('div');
        wrap.className = 'toast-wrap';
        document.body.appendChild(wrap);
    }
    const t = document.createElement('div');
    t.className = 'toast ' + type;
    t.textContent = msg;
    wrap.appendChild(t);
    setTimeout(() => t.remove(), 2600);
}

/* ============ 弹窗 ============ */
function openModal(title, bodyHtml, footerHtml, large = false) {
    closeModal();
    const mask = document.createElement('div');
    mask.className = 'modal-mask';
    mask.id = 'modal-mask';
    mask.innerHTML = `
        <div class="modal ${large ? 'modal-lg' : ''}">
            <div class="modal-header"><span>${title}</span>
                <button class="modal-close" onclick="closeModal()">×</button></div>
            <div class="modal-body">${bodyHtml}</div>
            ${footerHtml ? `<div class="modal-footer">${footerHtml}</div>` : ''}
        </div>`;
    mask.addEventListener('click', e => { if (e.target === mask) closeModal(); });
    document.body.appendChild(mask);
}

function closeModal() {
    const m = document.getElementById('modal-mask');
    if (m) m.remove();
}

function confirmDialog(msg, onOk) {
    openModal('确认操作', `<p style="font-size:14px">${msg}</p>`,
        `<button class="btn btn-plain" onclick="closeModal()">取消</button>
         <button class="btn btn-pink" id="confirm-ok">确定</button>`);
    document.getElementById('confirm-ok').onclick = () => { closeModal(); onOk(); };
}

/* ============ 抽屉 ============ */
function openDrawer(title, bodyHtml) {
    closeDrawer();
    const mask = document.createElement('div');
    mask.className = 'drawer-mask';
    mask.id = 'drawer-mask';
    mask.addEventListener('click', closeDrawer);
    const drawer = document.createElement('div');
    drawer.className = 'drawer';
    drawer.id = 'drawer';
    drawer.innerHTML = `
        <div class="drawer-header"><span>${title}</span>
            <button class="modal-close" onclick="closeDrawer()">×</button></div>
        <div class="drawer-body">${bodyHtml}</div>`;
    document.body.appendChild(mask);
    document.body.appendChild(drawer);
}

function closeDrawer() {
    const d = document.getElementById('drawer');
    const dm = document.getElementById('drawer-mask');
    if (d) d.remove();
    if (dm) dm.remove();
}

/* ============ 图片预览 ============ */
function previewImage(url) {
    const mask = document.createElement('div');
    mask.className = 'img-preview-mask';
    mask.innerHTML = `<img src="${url}">`;
    mask.addEventListener('click', () => mask.remove());
    document.body.appendChild(mask);
}

/* ============ 分页 ============ */
function renderPagination(containerId, page, total, size, onPage) {
    const el = document.getElementById(containerId);
    if (!el) return;
    const pages = Math.max(Math.ceil(total / size), 1);
    el.innerHTML = `
        <span>共 ${total} 条，第 ${page}/${pages} 页</span>
        <button ${page <= 1 ? 'disabled' : ''} onclick="void(0)" id="${containerId}-prev">上一页</button>
        <button ${page >= pages ? 'disabled' : ''} id="${containerId}-next">下一页</button>`;
    el.querySelector(`#${containerId}-prev`).onclick = () => page > 1 && onPage(page - 1);
    el.querySelector(`#${containerId}-next`).onclick = () => page < pages && onPage(page + 1);
}

/* ============ 字典 ============ */
const DEVICE_TYPES = ['路由器', '交换机', '防火墙', '无线AP', '服务器', '负载均衡', 'UPS', '摄像头', '其他'];

const DEVICE_STATUS = {
    IN_USE: { label: '在用', cls: 'badge-green' },
    IDLE: { label: '闲置', cls: 'badge-sand' },
    FAULT: { label: '故障', cls: 'badge-clay' },
    REPAIR: { label: '维修中', cls: 'badge-lavender' },
    LOANED: { label: '已借出', cls: 'badge-primary' },
    RETIRED: { label: '已报废', cls: 'badge-gray' },
};

const MAINT_STATUS = {
    PENDING: { label: '待受理', cls: 'badge-sand' },
    PROCESSING: { label: '处理中', cls: 'badge-lavender' },
    DONE: { label: '已完成', cls: 'badge-green' },
    REJECTED: { label: '已驳回', cls: 'badge-gray' },
};

const LOAN_STATUS = {
    PENDING: { label: '待审批', cls: 'badge-sand' },
    APPROVED: { label: '已借出', cls: 'badge-primary' },
    REJECTED: { label: '已驳回', cls: 'badge-gray' },
    RETURNED: { label: '已归还', cls: 'badge-green' },
};

function statusBadge(map, key) {
    const s = map[key] || { label: key || '-', cls: 'badge-gray' };
    return `<span class="badge ${s.cls}">${s.label}</span>`;
}

function esc(s) {
    if (s === null || s === undefined) return '';
    return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function fmtDate(s) { return s ? String(s).substring(0, 10) : '-'; }
function fmtTime(s) { return s ? String(s).substring(0, 16).replace('T', ' ') : '-'; }

/* ============ 图片上传组件 ============ */
/**
 * 用法：
 *   const up = new ImageUploader(containerEl, 'DEVICE');
 *   up.setExisting(images);        // 编辑时载入已有图
 *   // 保存实体后：await up.flush(bizId);
 */
class ImageUploader {
    constructor(container, bizType) {
        this.container = container;
        this.bizType = bizType;
        this.existing = [];   // 已保存的图片对象
        this.pending = [];    // 待上传 File
        this.render();
    }

    setExisting(images) {
        this.existing = images || [];
        this.render();
    }

    render() {
        const c = this.container;
        c.innerHTML = '';
        c.className = 'img-uploader';
        this.existing.forEach(img => {
            const item = document.createElement('div');
            item.className = 'img-item';
            item.innerHTML = `<img src="${img.url}" alt="">
                <button class="del" title="删除">×</button>`;
            item.querySelector('img').onclick = () => previewImage(img.url);
            item.querySelector('.del').onclick = async () => {
                if (!confirm('删除这张图片？')) return;
                try {
                    await request('/api/images/' + img.id, { method: 'DELETE' });
                    this.existing = this.existing.filter(x => x.id !== img.id);
                    this.render();
                    toast('图片已删除', 'success');
                } catch (e) { toast(e.message, 'error'); }
            };
            c.appendChild(item);
        });
        this.pending.forEach((file, idx) => {
            const item = document.createElement('div');
            item.className = 'img-item';
            const img = document.createElement('img');
            img.src = URL.createObjectURL(file);
            item.appendChild(img);
            const del = document.createElement('button');
            del.className = 'del';
            del.textContent = '×';
            del.onclick = () => { this.pending.splice(idx, 1); this.render(); };
            item.appendChild(del);
            c.appendChild(item);
        });
        const add = document.createElement('div');
        add.className = 'add-btn';
        add.innerHTML = '<span class="plus">+</span><span>添加图片</span>';
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'image/*';
        input.multiple = true;
        input.style.display = 'none';
        input.onchange = () => {
            Array.from(input.files || []).forEach(f => {
                if (f.size > 20 * 1024 * 1024) { toast(`图片 ${f.name} 超过 20MB，已跳过`, 'error'); return; }
                this.pending.push(f);
            });
            this.render();
        };
        add.onclick = () => input.click();
        c.appendChild(add);
        c.appendChild(input);
    }

    /** 保存实体后调用：上传所有待传图片 */
    async flush(bizId) {
        for (const file of this.pending) {
            await uploadFile(file, this.bizType, bizId);
        }
        this.pending = [];
    }
}
