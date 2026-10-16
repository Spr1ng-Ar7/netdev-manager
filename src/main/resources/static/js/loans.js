let curPage = 1;
const pageSize = 10;
let isAdmin = false;
let listCache = [];

authGuard('loans').then(init);

function init() {
    isAdmin = currentUser.role === 'ADMIN';
    document.getElementById('f-status').addEventListener('change', () => loadList(1));
    loadList(1);
}

function isOverdue(loan) {
    if (loan.status !== 'APPROVED' || !loan.expectReturnDate) return false;
    return loan.expectReturnDate < new Date().toISOString().substring(0, 10);
}

async function loadList(page = 1) {
    curPage = page;
    const params = new URLSearchParams({ page, size: pageSize });
    const status = document.getElementById('f-status').value;
    if (status) params.set('status', status);
    let data;
    try {
        data = await request('/api/loans?' + params.toString());
    } catch (e) { toast(e.message, 'error'); return; }
    listCache = data.list;

    const tbody = document.getElementById('tbody');
    if (!data.list.length) {
        tbody.innerHTML = '<tr><td colspan="9"><div class="empty">暂无借还记录</div></td></tr>';
    } else {
        tbody.innerHTML = data.list.map(l => `
            <tr>
                <td>#${l.id}</td>
                <td>${l.device ? esc(l.device.name) : '-'}</td>
                <td>${esc(l.borrower)}</td>
                <td title="${esc(l.purpose || '')}">${esc((l.purpose || '-').substring(0, 16))}${(l.purpose || '').length > 16 ? '…' : ''}</td>
                <td>${l.applicant ? esc(l.applicant.name) : '-'}</td>
                <td>${fmtDate(l.expectReturnDate)} ${isOverdue(l) ? '<span class="badge badge-clay">已超期</span>' : ''}</td>
                <td>${statusBadge(LOAN_STATUS, l.status)}</td>
                <td>${fmtDate(l.returnDate)}</td>
                <td class="ops">
                    ${isAdmin && l.status === 'PENDING' ? `
                        <button class="btn btn-green btn-sm" onclick="openApprove(${l.id})">通过</button>
                        <button class="btn btn-pink btn-sm" onclick="openReject(${l.id})">驳回</button>` : ''}
                    ${l.status === 'APPROVED' ? `<button class="btn btn-sm" onclick="openReturn(${l.id})">归还登记</button>` : ''}
                </td>
            </tr>`).join('');
    }
    renderPagination('pager', page, data.total, pageSize, loadList);
}

/* ============ 提交借出申请 ============ */
async function openApplyForm() {
    let devices;
    try {
        devices = await request('/api/devices/options');
    } catch (e) { toast(e.message, 'error'); return; }
    const options = devices
        .filter(d => d.status === 'IN_USE' || d.status === 'IDLE')
        .map(d => `<option value="${d.id}">${esc(d.name)}（${esc(d.type)}）</option>`).join('');
    openModal('提交借出申请', `
        <div class="form-grid">
            <div class="form-item full"><label>借用设备<span class="req">*</span></label>
                <select class="select" id="l-device"><option value="">请选择设备</option>${options}</select></div>
            <div class="form-item"><label>借用人<span class="req">*</span></label>
                <input class="input" id="l-borrower" placeholder="如：市场部-李强"></div>
            <div class="form-item"><label>预计归还日期<span class="req">*</span></label>
                <input class="input" type="date" id="l-date"></div>
            <div class="form-item full"><label>借用用途</label>
                <textarea class="textarea" id="l-purpose" placeholder="选填"></textarea></div>
        </div>`,
        `<button class="btn btn-plain" onclick="closeModal()">取消</button>
         <button class="btn btn-green" id="l-save">提交申请</button>`);
    document.getElementById('l-save').onclick = async () => {
        const deviceId = document.getElementById('l-device').value;
        const borrower = document.getElementById('l-borrower').value.trim();
        const expectReturnDate = document.getElementById('l-date').value;
        if (!deviceId) { toast('请选择设备', 'error'); return; }
        if (!borrower) { toast('请填写借用人', 'error'); return; }
        if (!expectReturnDate) { toast('请选择预计归还日期', 'error'); return; }
        try {
            await request('/api/loans', {
                method: 'POST',
                body: {
                    deviceId: +deviceId, borrower, expectReturnDate,
                    purpose: document.getElementById('l-purpose').value.trim() || null
                }
            });
            toast('借出申请已提交', 'success');
            closeModal();
            loadList(curPage);
        } catch (e) { toast(e.message, 'error'); }
    };
}

/* ============ 审批 ============ */
function openApprove(id) {
    openModal('审批通过', `
        <div class="form-item"><label>审批备注</label>
            <textarea class="textarea" id="l-remark" placeholder="选填"></textarea></div>`,
        `<button class="btn btn-plain" onclick="closeModal()">取消</button>
         <button class="btn btn-green" id="l-ok">确认通过</button>`);
    document.getElementById('l-ok').onclick = async () => {
        try {
            await request(`/api/loans/${id}/approve`, {
                method: 'PUT',
                body: { remark: document.getElementById('l-remark').value.trim() || null }
            });
            toast('已通过，设备状态更新为已借出', 'success');
            closeModal();
            loadList(curPage);
        } catch (e) { toast(e.message, 'error'); }
    };
}

function openReject(id) {
    openModal('驳回申请', `
        <div class="form-item"><label>驳回原因</label>
            <textarea class="textarea" id="l-remark" placeholder="选填"></textarea></div>`,
        `<button class="btn btn-plain" onclick="closeModal()">取消</button>
         <button class="btn btn-pink" id="l-ok">确认驳回</button>`);
    document.getElementById('l-ok').onclick = async () => {
        try {
            await request(`/api/loans/${id}/reject`, {
                method: 'PUT',
                body: { remark: document.getElementById('l-remark').value.trim() || null }
            });
            toast('已驳回', 'success');
            closeModal();
            loadList(curPage);
        } catch (e) { toast(e.message, 'error'); }
    };
}

/* ============ 归还登记 ============ */
function openReturn(id) {
    openModal('归还登记', `
        <div class="form-item"><label>归还备注</label>
            <textarea class="textarea" id="l-remark" placeholder="如：设备完好，配件齐全（选填）"></textarea></div>`,
        `<button class="btn btn-plain" onclick="closeModal()">取消</button>
         <button class="btn btn-green" id="l-ok">确认归还</button>`);
    document.getElementById('l-ok').onclick = async () => {
        try {
            await request(`/api/loans/${id}/return`, {
                method: 'PUT',
                body: { remark: document.getElementById('l-remark').value.trim() || null }
            });
            toast('归还登记成功', 'success');
            closeModal();
            loadList(curPage);
        } catch (e) { toast(e.message, 'error'); }
    };
}
