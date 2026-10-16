let curPage = 1;
const pageSize = 10;
let isAdmin = false;
let applyUploader = null;

authGuard('maintenance').then(init);

async function init() {
    isAdmin = currentUser.role === 'ADMIN';
    document.getElementById('f-status').addEventListener('change', () => loadList(1));
    await loadList(1);
}

async function loadList(page = 1) {
    curPage = page;
    const params = new URLSearchParams({ page, size: pageSize });
    const status = document.getElementById('f-status').value;
    if (status) params.set('status', status);
    let data;
    try {
        data = await request('/api/maintenances?' + params.toString());
    } catch (e) { toast(e.message, 'error'); return; }

    const tbody = document.getElementById('tbody');
    if (!data.list.length) {
        tbody.innerHTML = '<tr><td colspan="9"><div class="empty">暂无维保工单</div></td></tr>';
    } else {
        tbody.innerHTML = data.list.map(m => `
            <tr>
                <td>#${m.id}</td>
                <td>${m.device ? esc(m.device.name) : '-'}</td>
                <td title="${esc(m.faultDesc)}">${esc(m.faultDesc.length > 24 ? m.faultDesc.substring(0, 24) + '…' : m.faultDesc)}</td>
                <td>${m.applicant ? esc(m.applicant.name) : '-'}</td>
                <td>${fmtTime(m.applyTime)}</td>
                <td>${statusBadge(MAINT_STATUS, m.status)}</td>
                <td>${m.handler ? esc(m.handler.name) : '-'}</td>
                <td>${fmtTime(m.finishTime)}</td>
                <td class="ops">
                    <button class="btn btn-plain btn-sm" onclick="showDetail(${m.id})">详情</button>
                    ${isAdmin && m.status === 'PENDING' ? `<button class="btn btn-sm" onclick="doAction(${m.id}, 'accept')">受理</button>
                        <button class="btn btn-pink btn-sm" onclick="openReject(${m.id})">驳回</button>` : ''}
                    ${isAdmin && (m.status === 'PROCESSING' || m.status === 'PENDING') ? `<button class="btn btn-green btn-sm" onclick="openFinish(${m.id})">完成</button>` : ''}
                </td>
            </tr>`).join('');
    }
    renderPagination('pager', page, data.total, pageSize, loadList);
}

/* ============ 提交申请 ============ */
async function openApplyForm() {
    let devices;
    try {
        devices = await request('/api/devices/options');
    } catch (e) { toast(e.message, 'error'); return; }
    const options = devices
        .filter(d => d.status !== 'RETIRED')
        .map(d => `<option value="${d.id}">${esc(d.name)}（${esc(d.type)}）</option>`).join('');
    openModal('提交维保申请', `
        <div class="form-grid">
            <div class="form-item full"><label>报修设备<span class="req">*</span></label>
                <select class="select" id="m-device"><option value="">请选择设备</option>${options}</select></div>
            <div class="form-item full"><label>故障描述<span class="req">*</span></label>
                <textarea class="textarea" id="m-desc" placeholder="请描述设备故障现象、发生时间等"></textarea></div>
            <div class="form-item full"><label>故障图片（可多张）</label>
                <div id="m-images"></div></div>
        </div>`,
        `<button class="btn btn-plain" onclick="closeModal()">取消</button>
         <button class="btn btn-green" id="m-save">提交申请</button>`);

    applyUploader = new ImageUploader(document.getElementById('m-images'), 'MAINT_REQUEST');

    document.getElementById('m-save').onclick = async () => {
        const deviceId = document.getElementById('m-device').value;
        const faultDesc = document.getElementById('m-desc').value.trim();
        if (!deviceId) { toast('请选择设备', 'error'); return; }
        if (!faultDesc) { toast('请填写故障描述', 'error'); return; }
        const btn = document.getElementById('m-save');
        btn.disabled = true;
        try {
            const saved = await request('/api/maintenances', { method: 'POST', body: { deviceId: +deviceId, faultDesc } });
            await applyUploader.flush(saved.id);
            toast('维保申请已提交', 'success');
            closeModal();
            loadList(curPage);
        } catch (e) {
            toast(e.message, 'error');
            btn.disabled = false;
        }
    };
}

/* ============ 受理 / 完成 / 驳回 ============ */
async function doAction(id, action) {
    try {
        await request(`/api/maintenances/${id}/${action}`, { method: 'PUT', body: {} });
        toast('操作成功', 'success');
        loadList(curPage);
    } catch (e) { toast(e.message, 'error'); }
}

function openFinish(id) {
    openModal('完成维修', `
        <div class="form-grid">
            <div class="form-item full"><label>维修结果<span class="req">*</span></label>
                <textarea class="textarea" id="m-result" placeholder="请填写维修过程与结果"></textarea></div>
            <div class="form-item"><label>维修费用（元）</label>
                <input class="input" id="m-cost" type="number" min="0" step="0.01" placeholder="0.00"></div>
        </div>`,
        `<button class="btn btn-plain" onclick="closeModal()">取消</button>
         <button class="btn btn-green" id="m-ok">确认完成</button>`);
    document.getElementById('m-ok').onclick = async () => {
        const result = document.getElementById('m-result').value.trim();
        const costStr = document.getElementById('m-cost').value;
        if (!result) { toast('请填写维修结果', 'error'); return; }
        try {
            await request(`/api/maintenances/${id}/finish`, {
                method: 'PUT',
                body: { result, cost: costStr ? +costStr : null }
            });
            toast('工单已完成', 'success');
            closeModal();
            loadList(curPage);
        } catch (e) { toast(e.message, 'error'); }
    };
}

function openReject(id) {
    openModal('驳回申请', `
        <div class="form-item"><label>驳回原因</label>
            <textarea class="textarea" id="m-reason" placeholder="选填"></textarea></div>`,
        `<button class="btn btn-plain" onclick="closeModal()">取消</button>
         <button class="btn btn-pink" id="m-ok">确认驳回</button>`);
    document.getElementById('m-ok').onclick = async () => {
        try {
            await request(`/api/maintenances/${id}/reject`, {
                method: 'PUT',
                body: { result: document.getElementById('m-reason').value.trim() || '驳回' }
            });
            toast('已驳回', 'success');
            closeModal();
            loadList(curPage);
        } catch (e) { toast(e.message, 'error'); }
    };
}

/* ============ 详情（含图片） ============ */
async function showDetail(id) {
    // 从当前页数据里找 + 拉图片
    let images = [];
    try {
        images = await request('/api/images?bizType=MAINT_REQUEST&bizId=' + id);
    } catch (e) { /* ignore */ }
    const params = new URLSearchParams({ page: 1, size: 100 });
    let data;
    try {
        data = await request('/api/maintenances?' + params.toString());
    } catch (e) { toast(e.message, 'error'); return; }
    const m = (data.list || []).find(x => x.id === id);
    if (!m) { toast('工单不存在', 'error'); return; }

    openDrawer(`维保工单 #${m.id}`, `
        <div style="margin-bottom:14px">${statusBadge(MAINT_STATUS, m.status)}</div>
        <div class="desc-grid">
            <div class="item"><span class="k">设备</span><span>${m.device ? esc(m.device.name) : '-'}</span></div>
            <div class="item"><span class="k">申请人</span><span>${m.applicant ? esc(m.applicant.name) : '-'}</span></div>
            <div class="item"><span class="k">申请时间</span><span>${fmtTime(m.applyTime)}</span></div>
            <div class="item"><span class="k">受理时间</span><span>${fmtTime(m.acceptTime)}</span></div>
            <div class="item"><span class="k">处理人</span><span>${m.handler ? esc(m.handler.name) : '-'}</span></div>
            <div class="item"><span class="k">完成时间</span><span>${fmtTime(m.finishTime)}</span></div>
            ${m.cost != null ? `<div class="item"><span class="k">维修费用</span><span>¥ ${m.cost}</span></div>` : ''}
        </div>
        <div class="card-title" style="margin-top:18px">故障描述</div>
        <div style="font-size:13px;white-space:pre-wrap">${esc(m.faultDesc)}</div>
        ${m.result ? `<div class="card-title" style="margin-top:16px">处理结果</div>
            <div style="font-size:13px;white-space:pre-wrap">${esc(m.result)}</div>` : ''}
        <div class="card-title" style="margin-top:18px">申请图片（${images.length}）</div>
        ${images.length
            ? `<div class="img-grid">${images.map(img => `<img src="${img.url}" onclick="previewImage('${img.url}')">`).join('')}</div>`
            : '<div class="empty" style="padding:14px 0">暂无图片</div>'}
    `);
}
