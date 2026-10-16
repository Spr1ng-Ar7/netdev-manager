let departments = [], locations = [];
let curPage = 1;
const pageSize = 10;
let isAdmin = false;
let formUploader = null;

authGuard('devices').then(init);

async function init() {
    isAdmin = currentUser.role === 'ADMIN';
    if (isAdmin) document.getElementById('btn-add').style.display = '';
    try {
        [departments, locations] = await Promise.all([
            request('/api/departments'), request('/api/locations')
        ]);
    } catch (e) { toast(e.message, 'error'); }

    const typeSel = document.getElementById('f-type');
    DEVICE_TYPES.forEach(t => typeSel.add(new Option(t, t)));
    const statusSel = document.getElementById('f-status');
    Object.entries(DEVICE_STATUS).forEach(([k, v]) => statusSel.add(new Option(v.label, k)));
    const deptSel = document.getElementById('f-dept');
    departments.forEach(d => deptSel.add(new Option(d.name, d.id)));
    const locSel = document.getElementById('f-loc');
    locations.forEach(l => locSel.add(new Option(l.name, l.id)));

    document.getElementById('f-keyword').addEventListener('keydown', e => e.key === 'Enter' && loadDevices(1));
    await loadDevices(1);
}

async function loadDevices(page = 1) {
    curPage = page;
    const params = new URLSearchParams({ page, size: pageSize });
    const kw = document.getElementById('f-keyword').value.trim();
    if (kw) params.set('keyword', kw);
    const type = document.getElementById('f-type').value;
    if (type) params.set('type', type);
    const status = document.getElementById('f-status').value;
    if (status) params.set('status', status);
    const dept = document.getElementById('f-dept').value;
    if (dept) params.set('departmentId', dept);
    const loc = document.getElementById('f-loc').value;
    if (loc) params.set('locationId', loc);

    let data;
    try {
        data = await request('/api/devices?' + params.toString());
    } catch (e) { toast(e.message, 'error'); return; }

    const tbody = document.getElementById('device-tbody');
    if (!data.list.length) {
        tbody.innerHTML = '<tr><td colspan="10"><div class="empty">暂无设备数据</div></td></tr>';
    } else {
        tbody.innerHTML = data.list.map(d => `
            <tr>
                <td><a href="javascript:void(0)" onclick="showDetail(${d.id})">${esc(d.name)}</a></td>
                <td>${esc(d.type)}</td>
                <td>${esc(d.brand || '-')} / ${esc(d.model || '-')}</td>
                <td>${esc(d.ip || '-')}</td>
                <td>${d.department ? esc(d.department.name) : '-'}</td>
                <td>${d.location ? esc(d.location.name) : '-'}</td>
                <td>${esc(d.manager || '-')}</td>
                <td>${statusBadge(DEVICE_STATUS, d.status)}</td>
                <td>${fmtDate(d.warrantyExpire)}</td>
                <td class="ops">
                    <button class="btn btn-plain btn-sm" onclick="showDetail(${d.id})">详情</button>
                    ${isAdmin ? `
                    <button class="btn btn-sm" onclick="openDeviceForm(${d.id})">编辑</button>
                    <button class="btn btn-pink btn-sm" onclick="deleteDevice(${d.id}, '${esc(d.name)}')">删除</button>` : ''}
                </td>
            </tr>`).join('');
    }
    renderPagination('pager', page, data.total, pageSize, loadDevices);
}

function resetFilters() {
    ['f-keyword', 'f-type', 'f-status', 'f-dept', 'f-loc'].forEach(id => document.getElementById(id).value = '');
    loadDevices(1);
}

/* ============ 新增 / 编辑 ============ */
async function openDeviceForm(id) {
    let device = null;
    let images = [];
    if (id) {
        try {
            const detail = await request('/api/devices/' + id);
            device = detail.device;
            images = detail.images;
        } catch (e) { toast(e.message, 'error'); return; }
    }

    const deptOptions = departments.map(d =>
        `<option value="${d.id}" ${device && device.department && device.department.id === d.id ? 'selected' : ''}>${esc(d.name)}</option>`).join('');
    const locOptions = locations.map(l =>
        `<option value="${l.id}" ${device && device.location && device.location.id === l.id ? 'selected' : ''}>${esc(l.name)}</option>`).join('');
    const typeOptions = DEVICE_TYPES.map(t =>
        `<option value="${t}" ${device && device.type === t ? 'selected' : ''}>${t}</option>`).join('');
    const statusOptions = Object.entries(DEVICE_STATUS).map(([k, v]) =>
        `<option value="${k}" ${device && device.status === k ? 'selected' : ''}>${v.label}</option>`).join('');

    openModal(id ? '编辑设备' : '新增设备', `
        <div class="form-grid">
            <div class="form-item"><label>设备名称<span class="req">*</span></label>
                <input class="input" id="d-name" value="${esc(device ? device.name : '')}"></div>
            <div class="form-item"><label>设备类型<span class="req">*</span></label>
                <select class="select" id="d-type"><option value="">请选择</option>${typeOptions}</select></div>
            <div class="form-item"><label>品牌</label>
                <input class="input" id="d-brand" value="${esc(device ? device.brand || '' : '')}"></div>
            <div class="form-item"><label>型号</label>
                <input class="input" id="d-model" value="${esc(device ? device.model || '' : '')}"></div>
            <div class="form-item"><label>IP 地址</label>
                <input class="input" id="d-ip" value="${esc(device ? device.ip || '' : '')}"></div>
            <div class="form-item"><label>MAC 地址</label>
                <input class="input" id="d-mac" value="${esc(device ? device.mac || '' : '')}"></div>
            <div class="form-item"><label>序列号</label>
                <input class="input" id="d-sn" value="${esc(device ? device.serialNumber || '' : '')}"></div>
            <div class="form-item"><label>状态</label>
                <select class="select" id="d-status">${statusOptions}</select></div>
            <div class="form-item"><label>所属部门</label>
                <select class="select" id="d-dept"><option value="">无</option>${deptOptions}</select></div>
            <div class="form-item"><label>所在位置</label>
                <select class="select" id="d-loc"><option value="">无</option>${locOptions}</select></div>
            <div class="form-item"><label>负责人</label>
                <input class="input" id="d-manager" value="${esc(device ? device.manager || '' : '')}"></div>
            <div class="form-item"><label>购买日期</label>
                <input class="input" type="date" id="d-purchase" value="${device ? fmtDate(device.purchaseDate) : ''}"></div>
            <div class="form-item"><label>保修到期</label>
                <input class="input" type="date" id="d-warranty" value="${device ? fmtDate(device.warrantyExpire) : ''}"></div>
            <div class="form-item full"><label>备注</label>
                <textarea class="textarea" id="d-remark">${esc(device ? device.remark || '' : '')}</textarea></div>
            <div class="form-item full"><label>设备图片（可多张）</label>
                <div id="d-images"></div></div>
        </div>`,
        `<button class="btn btn-plain" onclick="closeModal()">取消</button>
         <button class="btn btn-green" id="d-save">保存</button>`, true);

    formUploader = new ImageUploader(document.getElementById('d-images'), 'DEVICE');
    if (images.length) formUploader.setExisting(images);

    document.getElementById('d-save').onclick = async () => {
        const body = {
            name: document.getElementById('d-name').value.trim(),
            type: document.getElementById('d-type').value,
            brand: document.getElementById('d-brand').value.trim() || null,
            model: document.getElementById('d-model').value.trim() || null,
            ip: document.getElementById('d-ip').value.trim() || null,
            mac: document.getElementById('d-mac').value.trim() || null,
            serialNumber: document.getElementById('d-sn').value.trim() || null,
            status: document.getElementById('d-status').value,
            manager: document.getElementById('d-manager').value.trim() || null,
            purchaseDate: document.getElementById('d-purchase').value || null,
            warrantyExpire: document.getElementById('d-warranty').value || null,
            remark: document.getElementById('d-remark').value.trim() || null,
            departmentId: document.getElementById('d-dept').value || null,
            locationId: document.getElementById('d-loc').value || null,
        };
        if (!body.name) { toast('请填写设备名称', 'error'); return; }
        if (!body.type) { toast('请选择设备类型', 'error'); return; }
        const btn = document.getElementById('d-save');
        btn.disabled = true;
        try {
            const saved = id
                ? await request('/api/devices/' + id, { method: 'PUT', body })
                : await request('/api/devices', { method: 'POST', body });
            await formUploader.flush(saved.id);
            toast('保存成功', 'success');
            closeModal();
            loadDevices(curPage);
        } catch (e) {
            toast(e.message, 'error');
            btn.disabled = false;
        }
    };
}

/* ============ 删除 ============ */
function deleteDevice(id, name) {
    confirmDialog(`确定删除设备「${name}」？<br><span style="color:var(--text-light);font-size:12px">将同时删除其图片、维保、借还、巡检记录</span>`, async () => {
        try {
            await request('/api/devices/' + id, { method: 'DELETE' });
            toast('删除成功', 'success');
            loadDevices(curPage);
        } catch (e) { toast(e.message, 'error'); }
    });
}

/* ============ 详情抽屉 ============ */
async function showDetail(id) {
    let detail;
    try {
        detail = await request('/api/devices/' + id);
    } catch (e) { toast(e.message, 'error'); return; }
    const d = detail.device;
    const images = detail.images || [];
    const maints = detail.maintenances || [];

    openDrawer(`设备详情 - ${esc(d.name)}`, `
        <div style="margin-bottom:16px">${statusBadge(DEVICE_STATUS, d.status)}</div>
        <div class="desc-grid">
            <div class="item"><span class="k">设备类型</span><span>${esc(d.type)}</span></div>
            <div class="item"><span class="k">品牌/型号</span><span>${esc(d.brand || '-')} / ${esc(d.model || '-')}</span></div>
            <div class="item"><span class="k">IP 地址</span><span>${esc(d.ip || '-')}</span></div>
            <div class="item"><span class="k">MAC 地址</span><span>${esc(d.mac || '-')}</span></div>
            <div class="item"><span class="k">序列号</span><span>${esc(d.serialNumber || '-')}</span></div>
            <div class="item"><span class="k">负责人</span><span>${esc(d.manager || '-')}</span></div>
            <div class="item"><span class="k">所属部门</span><span>${d.department ? esc(d.department.name) : '-'}</span></div>
            <div class="item"><span class="k">所在位置</span><span>${d.location ? esc(d.location.name) : '-'}</span></div>
            <div class="item"><span class="k">购买日期</span><span>${fmtDate(d.purchaseDate)}</span></div>
            <div class="item"><span class="k">保修到期</span><span>${fmtDate(d.warrantyExpire)}</span></div>
            <div class="item"><span class="k">创建时间</span><span>${fmtTime(d.createdAt)}</span></div>
            <div class="item"><span class="k">更新时间</span><span>${fmtTime(d.updatedAt)}</span></div>
        </div>
        ${d.remark ? `<div style="margin-top:12px;font-size:13px;color:var(--text-light)">备注：${esc(d.remark)}</div>` : ''}

        <div class="card-title" style="margin-top:22px">设备图片（${images.length}）</div>
        ${images.length
            ? `<div class="img-grid">${images.map(img => `<img src="${img.url}" onclick="previewImage('${img.url}')">`).join('')}</div>`
            : '<div class="empty" style="padding:14px 0">暂无图片</div>'}

        <div class="card-title" style="margin-top:22px">维修历史（${maints.length}）</div>
        ${maints.length ? `<div class="timeline">${maints.map(m => `
            <div class="t-item">
                <div class="t-time">${fmtTime(m.applyTime)} 提交 ${statusBadge(MAINT_STATUS, m.status)}</div>
                <div class="t-title">${esc(m.faultDesc)}</div>
                ${m.result ? `<div class="t-desc">处理结果：${esc(m.result)}${m.cost ? '，费用 ¥' + m.cost : ''}</div>` : ''}
            </div>`).join('')}</div>`
            : '<div class="empty" style="padding:14px 0">暂无维修记录</div>'}
    `);
}
