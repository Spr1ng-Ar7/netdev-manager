let curPage = 1;
const pageSize = 10;
let isAdmin = false;
let formUploader = null;

authGuard('inspections').then(init);

function init() {
    isAdmin = currentUser.role === 'ADMIN';
    document.getElementById('f-result').addEventListener('change', () => loadList(1));
    loadList(1);
}

const INSPECT_RESULT = {
    NORMAL: { label: '正常', cls: 'badge-green' },
    ABNORMAL: { label: '异常', cls: 'badge-clay' },
};

async function loadList(page = 1) {
    curPage = page;
    const params = new URLSearchParams({ page, size: pageSize });
    const result = document.getElementById('f-result').value;
    if (result) params.set('result', result);
    let data;
    try {
        data = await request('/api/inspections?' + params.toString());
    } catch (e) { toast(e.message, 'error'); return; }

    const tbody = document.getElementById('tbody');
    if (!data.list.length) {
        tbody.innerHTML = '<tr><td colspan="7"><div class="empty">暂无巡检记录</div></td></tr>';
    } else {
        tbody.innerHTML = data.list.map(r => `
            <tr>
                <td>#${r.id}</td>
                <td>${r.device ? esc(r.device.name) : '-'}</td>
                <td>${statusBadge(INSPECT_RESULT, r.result)}</td>
                <td title="${esc(r.content || '')}">${esc((r.content || '-').substring(0, 30))}${(r.content || '').length > 30 ? '…' : ''}</td>
                <td>${r.inspector ? esc(r.inspector.name) : '-'}</td>
                <td>${fmtTime(r.inspectTime)}</td>
                <td class="ops">
                    <button class="btn btn-plain btn-sm" onclick="showImages(${r.id})">图片</button>
                    ${isAdmin ? `<button class="btn btn-pink btn-sm" onclick="deleteRecord(${r.id})">删除</button>` : ''}
                </td>
            </tr>`).join('');
    }
    renderPagination('pager', page, data.total, pageSize, loadList);
}

/* ============ 登记巡检 ============ */
async function openForm() {
    let devices;
    try {
        devices = await request('/api/devices/options');
    } catch (e) { toast(e.message, 'error'); return; }
    const options = devices.map(d => `<option value="${d.id}">${esc(d.name)}（${esc(d.type)}）</option>`).join('');
    openModal('登记巡检', `
        <div class="form-grid">
            <div class="form-item full"><label>巡检设备<span class="req">*</span></label>
                <select class="select" id="i-device"><option value="">请选择设备</option>${options}</select></div>
            <div class="form-item"><label>巡检结果<span class="req">*</span></label>
                <select class="select" id="i-result">
                    <option value="NORMAL">正常</option>
                    <option value="ABNORMAL">异常</option>
                </select></div>
            <div class="form-item"><label>巡检时间</label>
                <input class="input" type="datetime-local" id="i-time"></div>
            <div class="form-item full"><label>巡检内容</label>
                <textarea class="textarea" id="i-content" placeholder="如：设备运行状态、指示灯、温度、告警情况等"></textarea></div>
            <div class="form-item full"><label>巡检图片（可多张）</label>
                <div id="i-images"></div></div>
        </div>`,
        `<button class="btn btn-plain" onclick="closeModal()">取消</button>
         <button class="btn btn-green" id="i-save">保存</button>`);

    formUploader = new ImageUploader(document.getElementById('i-images'), 'INSPECTION');

    document.getElementById('i-save').onclick = async () => {
        const deviceId = document.getElementById('i-device').value;
        const result = document.getElementById('i-result').value;
        if (!deviceId) { toast('请选择设备', 'error'); return; }
        const timeStr = document.getElementById('i-time').value;
        const body = {
            deviceId: +deviceId,
            result,
            content: document.getElementById('i-content').value.trim() || null,
            inspectTime: timeStr ? timeStr : null,
        };
        const btn = document.getElementById('i-save');
        btn.disabled = true;
        try {
            const saved = await request('/api/inspections', { method: 'POST', body });
            await formUploader.flush(saved.id);
            toast('巡检登记成功', 'success');
            closeModal();
            loadList(curPage);
        } catch (e) {
            toast(e.message, 'error');
            btn.disabled = false;
        }
    };
}

/* ============ 查看图片 ============ */
async function showImages(id) {
    let images = [];
    try {
        images = await request('/api/images?bizType=INSPECTION&bizId=' + id);
    } catch (e) { toast(e.message, 'error'); return; }
    openModal(`巡检图片 #${id}`,
        images.length
            ? `<div class="img-grid">${images.map(img => `<img src="${img.url}" onclick="previewImage('${img.url}')">`).join('')}</div>`
            : '<div class="empty">该巡检记录暂无图片</div>',
        `<button class="btn btn-plain" onclick="closeModal()">关闭</button>`);
}

/* ============ 删除 ============ */
function deleteRecord(id) {
    confirmDialog('确定删除这条巡检记录？', async () => {
        try {
            await request('/api/inspections/' + id, { method: 'DELETE' });
            toast('删除成功', 'success');
            loadList(curPage);
        } catch (e) { toast(e.message, 'error'); }
    });
}
